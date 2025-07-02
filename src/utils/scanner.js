
// Barcode scanning utility
export class BarcodeScanner {
    constructor(toastManager) {
        this.toastManager = toastManager;
        this.codeReader = null;
        this.isInitialized = false;
        this.initializeReader();
    }

    async initializeReader() {
        try {
            // ZXing 라이브러리가 로드될 때까지 대기
            let attempts = 0;
            while (!window.ZXing && attempts < 50) {
                await new Promise(resolve => setTimeout(resolve, 100));
                attempts++;
            }

            if (window.ZXing && window.ZXing.BrowserMultiFormatReader) {
                this.codeReader = new window.ZXing.BrowserMultiFormatReader();
                this.isInitialized = true;
                console.log('ZXing 바코드 리더 초기화 완료');
            } else {
                console.error('ZXing 라이브러리를 찾을 수 없습니다');
                this.toastManager.showToast('바코드 스캐너 초기화에 실패했습니다 🌽', 'error');
            }
        } catch (error) {
            console.error('ZXing 초기화 오류:', error);
            this.toastManager.showToast('바코드 스캐너 초기화 오류 🌽', 'error');
        }
    }

    async scanBarcode(imageSrc) {
        const scanOverlay = document.getElementById('scan-overlay');
        if (scanOverlay) {
            scanOverlay.classList.remove('hidden');
        }
        
        try {
            // 초기화가 안되어 있으면 다시 시도
            if (!this.isInitialized || !this.codeReader) {
                await this.initializeReader();
                if (!this.isInitialized || !this.codeReader) {
                    throw new Error('바코드 리더를 초기화할 수 없습니다');
                }
            }

            return new Promise((resolve, reject) => {
                const img = new Image();
                img.crossOrigin = 'anonymous';
                
                img.onload = async () => {
                    try {
                        // 캔버스 생성 및 이미지 그리기
                        const canvas = document.createElement('canvas');
                        const ctx = canvas.getContext('2d');
                        
                        // 원본 크기로 캔버스 설정
                        canvas.width = img.naturalWidth || img.width;
                        canvas.height = img.naturalHeight || img.height;
                        
                        // 이미지를 캔버스에 그리기
                        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
                        
                        // 이미지 데이터 추출
                        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                        
                        console.log('바코드 스캔 시작 - 이미지 크기:', canvas.width, 'x', canvas.height);
                        
                        // 여러 방법으로 바코드 스캔 시도
                        let result = null;
                        
                        // 1. 원본 이미지로 스캔
                        try {
                            result = await this.codeReader.decodeFromImageData(imageData);
                            console.log('원본 이미지에서 바코드 인식 성공:', result.text);
                        } catch (e) {
                            console.log('원본 이미지 스캔 실패, 다른 방법 시도...');
                        }
                        
                        // 2. 이미지 크기 조정해서 스캔 (더 큰 크기)
                        if (!result && canvas.width < 1000) {
                            try {
                                const scaleFactor = Math.min(1000 / canvas.width, 1000 / canvas.height);
                                const scaledCanvas = document.createElement('canvas');
                                const scaledCtx = scaledCanvas.getContext('2d');
                                scaledCanvas.width = canvas.width * scaleFactor;
                                scaledCanvas.height = canvas.height * scaleFactor;
                                
                                scaledCtx.drawImage(img, 0, 0, scaledCanvas.width, scaledCanvas.height);
                                const scaledImageData = scaledCtx.getImageData(0, 0, scaledCanvas.width, scaledCanvas.height);
                                
                                result = await this.codeReader.decodeFromImageData(scaledImageData);
                                console.log('확대된 이미지에서 바코드 인식 성공:', result.text);
                            } catch (e) {
                                console.log('확대된 이미지 스캔 실패');
                            }
                        }
                        
                        // 3. 컨트라스트 조정해서 스캔
                        if (!result) {
                            try {
                                const contrastCanvas = document.createElement('canvas');
                                const contrastCtx = contrastCanvas.getContext('2d');
                                contrastCanvas.width = canvas.width;
                                contrastCanvas.height = canvas.height;
                                
                                contrastCtx.filter = 'contrast(150%) brightness(110%)';
                                contrastCtx.drawImage(img, 0, 0, canvas.width, canvas.height);
                                
                                const contrastImageData = contrastCtx.getImageData(0, 0, canvas.width, canvas.height);
                                result = await this.codeReader.decodeFromImageData(contrastImageData);
                                console.log('컨트라스트 조정된 이미지에서 바코드 인식 성공:', result.text);
                            } catch (e) {
                                console.log('컨트라스트 조정된 이미지 스캔 실패');
                            }
                        }
                        
                        if (scanOverlay) {
                            scanOverlay.classList.add('hidden');
                        }
                        
                        if (result && result.text) {
                            this.toastManager.showToast('바코드 인식 성공! 🌽✨', 'success');
                            resolve(result.text);
                        } else {
                            console.log('모든 방법으로 바코드 스캔 실패');
                            this.toastManager.showToast('바코드를 인식할 수 없습니다. 수동으로 입력해주세요 🌽', 'warning');
                            resolve(null);
                        }
                        
                    } catch (error) {
                        console.error('바코드 스캔 중 오류:', error);
                        if (scanOverlay) {
                            scanOverlay.classList.add('hidden');
                        }
                        this.toastManager.showToast('바코드를 인식할 수 없습니다. 수동으로 입력해주세요 🌽', 'warning');
                        resolve(null);
                    }
                };
                
                img.onerror = (error) => {
                    console.error('이미지 로드 오류:', error);
                    if (scanOverlay) {
                        scanOverlay.classList.add('hidden');
                    }
                    this.toastManager.showToast('이미지 로드에 실패했습니다 🌽', 'error');
                    resolve(null);
                };
                
                // 이미지 로드 시작
                if (imageSrc.startsWith('data:')) {
                    img.src = imageSrc;
                } else {
                    // URL인 경우 CORS 처리
                    img.src = imageSrc;
                }
            });
            
        } catch (error) {
            if (scanOverlay) {
                scanOverlay.classList.add('hidden');
            }
            console.error('스캔 프로세스 오류:', error);
            this.toastManager.showToast('스캔 중 오류가 발생했습니다 🌽', 'error');
            return null;
        }
    }
}
