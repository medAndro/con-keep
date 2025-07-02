
// Coupon sharing utility
export class SharingManager {
    constructor(toastManager) {
        this.toastManager = toastManager;
        this.sharedCoupons = new Map();
        this.loadSharedCoupons();
    }

    loadSharedCoupons() {
        try {
            const stored = localStorage.getItem('sharedCoupons');
            if (stored) {
                const parsed = JSON.parse(stored);
                this.sharedCoupons = new Map(Object.entries(parsed));
            }
        } catch (error) {
            console.error('공유 쿠폰 로드 오류:', error);
        }
    }

    saveSharedCoupons() {
        try {
            const obj = Object.fromEntries(this.sharedCoupons);
            localStorage.setItem('sharedCoupons', JSON.stringify(obj));
        } catch (error) {
            console.error('공유 쿠폰 저장 오류:', error);
        }
    }

    shareCoupon(coupon) {
        if (!coupon) {
            this.toastManager.showToast('공유할 쿠폰이 없습니다 🌽', 'error');
            return;
        }

        // 공유 쿠폰을 저장
        this.sharedCoupons.set(coupon.id, {
            ...coupon,
            sharedAt: Date.now()
        });
        this.saveSharedCoupons();

        const shareUrl = `${window.location.origin}${window.location.pathname}?share=${coupon.id}`;
        
        // QR 코드 생성
        const canvas = document.getElementById('qr-canvas');
        if (canvas && typeof QRCode !== 'undefined') {
            QRCode.toCanvas(canvas, shareUrl, { width: 200 }, (error) => {
                if (error) {
                    console.error('QR 코드 생성 오류:', error);
                    this.toastManager.showToast('QR 코드 생성에 실패했습니다 🌽', 'error');
                } else {
                    console.log('QR 코드 생성 완료');
                }
            });
        } else {
            console.error('QR 코드 캔버스 또는 QRCode 라이브러리를 찾을 수 없습니다');
        }

        const shareUrlInput = document.getElementById('share-url');
        if (shareUrlInput) {
            shareUrlInput.value = shareUrl;
        }

        const shareSection = document.getElementById('share-section');
        if (shareSection) {
            shareSection.classList.remove('hidden');
        }
        
        this.toastManager.showToast('공유 링크가 생성되었습니다! 🌽🔗', 'success');
    }

    copyShareLink() {
        const shareUrl = document.getElementById('share-url');
        if (shareUrl) {
            shareUrl.select();
            shareUrl.setSelectionRange(0, 99999); // 모바일 호환성
            
            try {
                document.execCommand('copy');
                this.toastManager.showToast('링크가 복사되었습니다! 🌽📋', 'success');
            } catch (error) {
                console.error('복사 오류:', error);
                this.toastManager.showToast('링크 복사에 실패했습니다 🌽', 'error');
            }
        }
    }

    getSharedCoupon(shareId) {
        return this.sharedCoupons.get(shareId);
    }

    updateSharedCoupon(shareId, coupon) {
        this.sharedCoupons.set(shareId, coupon);
        this.saveSharedCoupons();
    }
}
