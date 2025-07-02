// 새로운 바코드 리더 구현 (ZXing-JS 기반)
export class BarcodeReader {
    constructor(toastManager) {
        this.toastManager = toastManager;
        this.isReady = false;
        this.reader = null;
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
                console.error('ZXing 라이브러리를 찾을 수 없습니다. 스크립트 로드 확인 필요.');
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
            const img = new Image();
            img.crossOrigin = 'anonymous'; // CORS 문제 방지
            img.src = imageSrc;

            await new Promise((resolve, reject) => {
                img.onload = resolve;
                img.onerror = reject;
            });

            // 여러 스캔 방법 시도
            const result = await this.tryMultipleScanMethods(img);
            
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

    async tryMultipleScanMethods(img) {
        const methods = [
            { name: 'Original', func: () => this.decodeFromImage(img) },
            { name: 'Contrast', func: () => this.decodeWithPreprocessing(img, 'contrast') },
            { name: 'Brightness', func: () => this.decodeWithPreprocessing(img, 'brightness') },
            { name: 'Grayscale', func: () => this.decodeWithPreprocessing(img, 'grayscale') },
            { name: 'Sharpening', func: () => this.decodeWithPreprocessing(img, 'sharpening') },
        ];

        for (const method of methods) {
            try {
                console.log(`스캔 시도: ${method.name}`);
                const result = await method.func();
                if (result) {
                    console.log(`스캔 성공: ${method.name}`);
                    return result;
                }
            } catch (error) {
                console.log(`스캔 방법 실패 (${method.name}):`, error.message);
                // 다음 방법 시도
            }
        }
        return null;
    }

    async decodeFromImage(img) {
        try {
            const result = await this.reader.decodeFromImage(img);
            return result.text;
        } catch (error) {
            return null;
        }
    }

    async decodeWithPreprocessing(img, type) {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        
        canvas.width = img.naturalWidth;
        canvas.height = img.naturalHeight;
        ctx.drawImage(img, 0, 0);

        let imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);

        switch (type) {
            case 'contrast':
                this.adjustContrast(imageData, 1.5);
                break;
            case 'brightness':
                this.adjustBrightness(imageData, 30);
                break;
            case 'grayscale':
                this.convertToGrayscale(imageData);
                break;
            case 'sharpening':
                this.applySharpeningFilter(imageData);
                break;
        }
        
        ctx.putImageData(imageData, 0, 0); // 변경된 이미지 데이터를 캔버스에 다시 그립니다.

        try {
            const result = await this.reader.decodeFromCanvas(canvas);
            return result.text;
        } catch (error) {
            return null;
        }
    }

    // 이미지 전처리 함수들 (이전 구현에서 가져옴)
    adjustContrast(imageData, contrast) {
        const data = imageData.data;
        const factor = (259 * (contrast * 255 + 255)) / (255 * (259 - contrast * 255));
        for (let i = 0; i < data.length; i += 4) {
            data[i] = this.clamp((data[i] - 128) * factor + 128);
            data[i + 1] = this.clamp((data[i + 1] - 128) * factor + 128);
            data[i + 2] = this.clamp((data[i + 2] - 128) * factor + 128);
        }
    }

    adjustBrightness(imageData, brightness) {
        const data = imageData.data;
        for (let i = 0; i < data.length; i += 4) {
            data[i] = this.clamp(data[i] + brightness);
            data[i + 1] = this.clamp(data[i + 1] + brightness);
            data[i + 2] = this.clamp(data[i + 2] + brightness);
        }
    }

    convertToGrayscale(imageData) {
        const data = imageData.data;
        for (let i = 0; i < data.length; i += 4) {
            const gray = data[i] * 0.299 + data[i + 1] * 0.587 + data[i + 2] * 0.114;
            data[i] = gray;
            data[i + 1] = gray;
            data[i + 2] = gray;
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
        // 원본 imageData에 newData를 복사
        imageData.data.set(newData);
    }

    clamp(value) {
        return Math.max(0, Math.min(255, value));
    }
}
