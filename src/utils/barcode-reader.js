
// 새로운 바코드 리더 구현
export class BarcodeReader {
    constructor(toastManager) {
        this.toastManager = toastManager;
        this.isReady = false;
        this.initializeLibrary();
    }

    async initializeLibrary() {
        try {
            // ZXing 라이브러리 로드 대기
            let attempts = 0;
            const maxAttempts = 100;
            
            while (!window.ZXing && attempts < maxAttempts) {
                await new Promise(resolve => setTimeout(resolve, 100));
                attempts++;
            }

            if (window.ZXing && window.ZXing.BrowserMultiFormatReader) {
                this.reader = new window.ZXing.BrowserMultiFormatReader();
                this.isReady = true;
                console.log('바코드 리더 초기화 완료');
                return true;
            } else {
                console.error('ZXing 라이브러리를 찾을 수 없습니다');
                return false;
            }
        } catch (error) {
            console.error('바코드 리더 초기화 오류:', error);
            return false;
        }
    }

    async processImage(imageSrc) {
        console.log('바코드 스캔 시작:', imageSrc ? '이미지 있음' : '이미지 없음');
        
        if (!this.isReady) {
            const initialized = await this.initializeLibrary();
            if (!initialized) {
                this.toastManager.showToast('바코드 리더 초기화 실패 🌽', 'error');
                return null;
            }
        }

        try {
            const canvas = await this.createCanvasFromImage(imageSrc);
            if (!canvas) {
                console.error('캔버스 생성 실패');
                return null;
            }

            // 여러 스캔 방법 시도
            const result = await this.tryMultipleScanMethods(canvas);
            
            if (result) {
                console.log('바코드 인식 성공:', result);
                this.toastManager.showToast('바코드 인식 성공! 🌽✨', 'success');
                return result;
            } else {
                console.log('바코드 인식 실패 - 모든 방법 시도됨');
                this.toastManager.showToast('바코드를 인식할 수 없습니다. 수동으로 입력해주세요 🌽', 'warning');
                return null;
            }
        } catch (error) {
            console.error('바코드 처리 오류:', error);
            this.toastManager.showToast('바코드 스캔 중 오류 발생 🌽', 'error');
            return null;
        }
    }

    async createCanvasFromImage(imageSrc) {
        return new Promise((resolve) => {
            const img = new Image();
            img.crossOrigin = 'anonymous';
            
            img.onload = () => {
                try {
                    const canvas = document.createElement('canvas');
                    const ctx = canvas.getContext('2d');
                    
                    // 적절한 크기로 캔버스 설정
                    const maxSize = 1200;
                    let { width, height } = img;
                    
                    if (width > maxSize || height > maxSize) {
                        const ratio = Math.min(maxSize / width, maxSize / height);
                        width *= ratio;
                        height *= ratio;
                    }
                    
                    canvas.width = width;
                    canvas.height = height;
                    
                    // 이미지를 캔버스에 그리기
                    ctx.drawImage(img, 0, 0, width, height);
                    
                    console.log('캔버스 생성 완료:', width, 'x', height);
                    resolve(canvas);
                } catch (error) {
                    console.error('캔버스 생성 오류:', error);
                    resolve(null);
                }
            };
            
            img.onerror = (error) => {
                console.error('이미지 로드 오류:', error);
                resolve(null);
            };
            
            img.src = imageSrc;
        });
    }

    async tryMultipleScanMethods(canvas) {
        const ctx = canvas.getContext('2d');
        const methods = [
            () => this.scanOriginal(ctx, canvas.width, canvas.height),
            () => this.scanWithContrast(ctx, canvas.width, canvas.height),
            () => this.scanWithBrightness(ctx, canvas.width, canvas.height),
            () => this.scanGrayscale(ctx, canvas.width, canvas.height),
            () => this.scanWithSharpening(ctx, canvas.width, canvas.height)
        ];

        for (const method of methods) {
            try {
                const result = await method();
                if (result) {
                    return result;
                }
            } catch (error) {
                console.log('스캔 방법 실패:', error.message);
                continue;
            }
        }
        
        return null;
    }

    async scanOriginal(ctx, width, height) {
        console.log('원본 이미지로 스캔 시도');
        const imageData = ctx.getImageData(0, 0, width, height);
        return await this.decodeImageData(imageData);
    }

    async scanWithContrast(ctx, width, height) {
        console.log('대비 조정하여 스캔 시도');
        const imageData = ctx.getImageData(0, 0, width, height);
        this.adjustContrast(imageData, 1.5);
        return await this.decodeImageData(imageData);
    }

    async scanWithBrightness(ctx, width, height) {
        console.log('밝기 조정하여 스캔 시도');
        const imageData = ctx.getImageData(0, 0, width, height);
        this.adjustBrightness(imageData, 30);
        return await this.decodeImageData(imageData);
    }

    async scanGrayscale(ctx, width, height) {
        console.log('흑백으로 변환하여 스캔 시도');
        const imageData = ctx.getImageData(0, 0, width, height);
        this.convertToGrayscale(imageData);
        return await this.decodeImageData(imageData);
    }

    async scanWithSharpening(ctx, width, height) {
        console.log('선명도 조정하여 스캔 시도');
        const imageData = ctx.getImageData(0, 0, width, height);
        this.applySharpeningFilter(imageData);
        return await this.decodeImageData(imageData);
    }

    async decodeImageData(imageData) {
        try {
            const result = await this.reader.decodeFromImageData(imageData);
            return result.text;
        } catch (error) {
            return null;
        }
    }

    adjustContrast(imageData, contrast) {
        const data = imageData.data;
        const factor = (259 * (contrast * 255 + 255)) / (255 * (259 - contrast * 255));
        
        for (let i = 0; i < data.length; i += 4) {
            data[i] = this.clamp((data[i] - 128) * factor + 128);     // Red
            data[i + 1] = this.clamp((data[i + 1] - 128) * factor + 128); // Green
            data[i + 2] = this.clamp((data[i + 2] - 128) * factor + 128); // Blue
        }
    }

    adjustBrightness(imageData, brightness) {
        const data = imageData.data;
        
        for (let i = 0; i < data.length; i += 4) {
            data[i] = this.clamp(data[i] + brightness);     // Red
            data[i + 1] = this.clamp(data[i + 1] + brightness); // Green
            data[i + 2] = this.clamp(data[i + 2] + brightness); // Blue
        }
    }

    convertToGrayscale(imageData) {
        const data = imageData.data;
        
        for (let i = 0; i < data.length; i += 4) {
            const gray = data[i] * 0.299 + data[i + 1] * 0.587 + data[i + 2] * 0.114;
            data[i] = gray;     // Red
            data[i + 1] = gray; // Green
            data[i + 2] = gray; // Blue
        }
    }

    applySharpeningFilter(imageData) {
        const data = imageData.data;
        const width = imageData.width;
        const height = imageData.height;
        const kernel = [
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0
        ];
        
        const newData = new Uint8ClampedArray(data);
        
        for (let y = 1; y < height - 1; y++) {
            for (let x = 1; x < width - 1; x++) {
                for (let c = 0; c < 3; c++) {
                    let sum = 0;
                    for (let ky = -1; ky <= 1; ky++) {
                        for (let kx = -1; kx <= 1; kx++) {
                            const idx = ((y + ky) * width + (x + kx)) * 4 + c;
                            const kernelIdx = (ky + 1) * 3 + (kx + 1);
                            sum += data[idx] * kernel[kernelIdx];
                        }
                    }
                    const idx = (y * width + x) * 4 + c;
                    newData[idx] = this.clamp(sum);
                }
            }
        }
        
        for (let i = 0; i < data.length; i++) {
            data[i] = newData[i];
        }
    }

    clamp(value) {
        return Math.max(0, Math.min(255, value));
    }
}
