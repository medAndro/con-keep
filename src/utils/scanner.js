
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
            // ZXing 라이브러리 로드 대기
            let attempts = 0;
            while (typeof ZXing === 'undefined' && attempts < 50) {
                await new Promise(resolve => setTimeout(resolve, 100));
                attempts++;
            }

            if (typeof ZXing !== 'undefined' && ZXing.BrowserMultiFormatReader) {
                this.codeReader = new ZXing.BrowserMultiFormatReader();
                this.isInitialized = true;
                console.log('ZXing 바코드 리더 초기화 완료');
            } else {
                console.error('ZXing 라이브러리를 찾을 수 없습니다');
                this.isInitialized = false;
            }
        } catch (error) {
            console.error('ZXing 초기화 오류:', error);
            this.isInitialized = false;
        }
    }

    async preprocessImage(canvas) {
        const ctx = canvas.getContext('2d');
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const data = imageData.data;

        // 이미지 전처리: 대비 증가 및 이진화
        for (let i = 0; i < data.length; i += 4) {
            const gray = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
            const enhanced = gray > 128 ? 255 : 0;
            data[i] = enhanced;     // R
            data[i + 1] = enhanced; // G
            data[i + 2] = enhanced; // B
        }

        ctx.putImageData(imageData, 0, 0);
        return canvas;
    }

    async scanBarcode(imageSrc) {
        const scanOverlay = document.getElementById('scan-overlay');
        if (scanOverlay) {
            scanOverlay.classList.remove('hidden');
        }
        
        try {
            // 초기화 확인
            if (!this.isInitialized) {
                await this.initializeReader();
                if (!this.isInitialized) {
                    throw new Error('바코드 리더를 초기화할 수 없습니다');
                }
            }

            const img = new Image();
            img.crossOrigin = 'anonymous';
            
            return new Promise((resolve, reject) => {
                img.onload = async () => {
                    try {
                        // 원본 이미지로 시도
                        let result = await this.tryDecodeImage(img);
                        
                        if (!result) {
                            // 전처리된 이미지로 시도
                            const processedCanvas = await this.createProcessedCanvas(img);
                            result = await this.tryDecodeCanvas(processedCanvas);
                        }

                        if (!result) {
                            // 다양한 크기로 시도
                            result = await this.tryMultipleScales(img);
                        }

                        if (scanOverlay) {
                            scanOverlay.classList.add('hidden');
                        }

                        if (result) {
                            this.toastManager.showToast('바코드 인식 성공! 🌽✨', 'success');
                            resolve(result);
                        } else {
                            this.toastManager.showToast('바코드를 인식할 수 없습니다. 수동으로 입력해주세요 🌽', 'warning');
                            resolve(null);
                        }
                        
                    } catch (error) {
                        console.error('바코드 스캔 오류:', error);
                        if (scanOverlay) {
                            scanOverlay.classList.add('hidden');
                        }
                        this.toastManager.showToast('바코드를 인식할 수 없습니다. 수동으로 입력해주세요 🌽', 'warning');
                        resolve(null);
                    }
                };
                
                img.onerror = () => {
                    if (scanOverlay) {
                        scanOverlay.classList.add('hidden');
                    }
                    this.toastManager.showToast('이미지 로드에 실패했습니다 🌽', 'error');
                    reject(new Error('이미지 로드 실패'));
                };
                
                img.src = imageSrc;
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

    async tryDecodeImage(img) {
        try {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            canvas.width = img.width;
            canvas.height = img.height;
            ctx.drawImage(img, 0, 0);
            
            const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
            const result = await this.codeReader.decodeFromImageData(imageData);
            return result.text;
        } catch (error) {
            console.log('원본 이미지 디코딩 실패:', error.message);
            return null;
        }
    }

    async createProcessedCanvas(img) {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        canvas.width = img.width;
        canvas.height = img.height;
        ctx.drawImage(img, 0, 0);
        
        return await this.preprocessImage(canvas);
    }

    async tryDecodeCanvas(canvas) {
        try {
            const ctx = canvas.getContext('2d');
            const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
            const result = await this.codeReader.decodeFromImageData(imageData);
            return result.text;
        } catch (error) {
            console.log('전처리 이미지 디코딩 실패:', error.message);
            return null;
        }
    }

    async tryMultipleScales(img) {
        const scales = [0.5, 1.5, 2.0, 0.25];
        
        for (const scale of scales) {
            try {
                const canvas = document.createElement('canvas');
                const ctx = canvas.getContext('2d');
                canvas.width = img.width * scale;
                canvas.height = img.height * scale;
                
                ctx.imageSmoothingEnabled = false;
                ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
                
                const result = await this.tryDecodeCanvas(canvas);
                if (result) {
                    console.log(`스케일 ${scale}에서 바코드 인식 성공`);
                    return result;
                }
            } catch (error) {
                console.log(`스케일 ${scale} 디코딩 실패:`, error.message);
            }
        }
        
        return null;
    }
}
