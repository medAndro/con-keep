
// 간소화된 공유 매니저
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
        
        return coupon.id;
    }

    getSharedCoupon(shareId) {
        return this.sharedCoupons.get(shareId);
    }

    updateSharedCoupon(shareId, coupon) {
        this.sharedCoupons.set(shareId, coupon);
        this.saveSharedCoupons();
    }
}
