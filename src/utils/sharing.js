
// Coupon sharing utility
export class SharingManager {
    constructor(toastManager) {
        this.toastManager = toastManager;
        this.sharedCoupons = new Map();
    }

    shareCoupon(coupon) {
        // 공유 쿠폰을 메모리에 저장
        this.sharedCoupons.set(coupon.id, {
            ...coupon,
            sharedAt: Date.now()
        });

        const shareUrl = `${window.location.origin}${window.location.pathname}?share=${coupon.id}`;
        
        // QR 코드 생성
        const canvas = document.getElementById('qr-canvas');
        QRCode.toCanvas(canvas, shareUrl, { width: 200 }, (error) => {
            if (error) {
                console.error('QR 코드 생성 오류:', error);
                this.toastManager.showToast('QR 코드 생성에 실패했습니다 🌽', 'error');
            }
        });

        document.getElementById('share-url').value = shareUrl;
        document.getElementById('share-section').classList.remove('hidden');
        
        this.toastManager.showToast('공유 링크가 생성되었습니다! 🌽🔗', 'success');
    }

    copyShareLink() {
        const shareUrl = document.getElementById('share-url');
        shareUrl.select();
        document.execCommand('copy');
        this.toastManager.showToast('링크가 복사되었습니다! 🌽📋', 'success');
    }

    getSharedCoupon(shareId) {
        return this.sharedCoupons.get(shareId);
    }

    updateSharedCoupon(shareId, coupon) {
        this.sharedCoupons.set(shareId, coupon);
    }
}
