
// 기존 스캐너를 새로운 바코드 리더로 교체
import { BarcodeReader } from './barcode-reader.js';

export class BarcodeScanner {
    constructor(toastManager) {
        this.toastManager = toastManager;
        this.barcodeReader = new BarcodeReader(toastManager);
    }

    async scanBarcode(imageSrc) {
        const scanOverlay = document.getElementById('scan-overlay');
        if (scanOverlay) {
            scanOverlay.classList.remove('hidden');
        }

        try {
            const result = await this.barcodeReader.processImage(imageSrc);
            return result;
        } finally {
            if (scanOverlay) {
                scanOverlay.classList.add('hidden');
            }
        }
    }
}
