
// Barcode scanning utility
export class BarcodeScanner {
    constructor(toastManager) {
        this.toastManager = toastManager;
    }

    async scanBarcode(imageSrc) {
        const scanOverlay = document.getElementById('scan-overlay');
        scanOverlay.classList.remove('hidden');
        
        try {
            const img = new Image();
            img.crossOrigin = 'anonymous';
            
            return new Promise((resolve, reject) => {
                img.onload = async () => {
                    try {
                        const canvas = document.createElement('canvas');
                        const ctx = canvas.getContext('2d');
                        canvas.width = img.width;
                        canvas.height = img.height;
                        ctx.drawImage(img, 0, 0);
                        
                        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                        
                        const codeReader = new ZXing.BrowserMultiFormatReader();
                        const result = await codeReader.decodeFromImageData(imageData);
                        
                        scanOverlay.classList.add('hidden');
                        this.toastManager.showToast('바코드 인식 성공! 🌽✨', 'success');
                        
                        resolve(result.text);
                        
                    } catch (error) {
                        console.error('바코드 스캔 오류:', error);
                        scanOverlay.classList.add('hidden');
                        this.toastManager.showToast('바코드를 인식할 수 없습니다. 수동으로 입력해주세요 🌽', 'warning');
                        resolve(null);
                    }
                };
                
                img.onerror = () => {
                    scanOverlay.classList.add('hidden');
                    this.toastManager.showToast('이미지 로드에 실패했습니다 🌽', 'error');
                    reject(new Error('이미지 로드 실패'));
                };
                
                img.src = imageSrc;
            });
            
        } catch (error) {
            scanOverlay.classList.add('hidden');
            console.error('스캔 프로세스 오류:', error);
            this.toastManager.showToast('스캔 중 오류가 발생했습니다 🌽', 'error');
        }
    }
}
