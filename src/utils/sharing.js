
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
        
        // 공유 URL 설정
        const shareUrlInput = document.getElementById('share-url');
        if (shareUrlInput) {
            shareUrlInput.value = shareUrl;
        }

        // 공유 섹션 표시
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

    shareToKakao() {
        const shareUrl = document.getElementById('share-url')?.value;
        if (!shareUrl) return;

        try {
            // 카카오톡 공유 (웹 브라우저에서 카카오톡 앱으로 전환)
            const kakaoUrl = `https://sharer.kakao.com/talk/friends/?url=${encodeURIComponent(shareUrl)}&text=${encodeURIComponent('🌽 기프티콘을 공유합니다!')}`;
            window.open(kakaoUrl, '_blank');
        } catch (error) {
            console.error('카카오톡 공유 오류:', error);
            this.copyShareLink(); // 실패시 링크 복사로 대체
        }
    }

    shareToSMS() {
        const shareUrl = document.getElementById('share-url')?.value;
        if (!shareUrl) return;

        try {
            const smsUrl = `sms:?body=${encodeURIComponent('🌽 기프티콘을 공유합니다: ' + shareUrl)}`;
            window.location.href = smsUrl;
        } catch (error) {
            console.error('SMS 공유 오류:', error);
            this.copyShareLink();
        }
    }

    shareToEmail() {
        const shareUrl = document.getElementById('share-url')?.value;
        if (!shareUrl) return;

        try {
            const emailUrl = `mailto:?subject=${encodeURIComponent('기프티콘 공유')}&body=${encodeURIComponent('🌽 기프티콘을 공유합니다: ' + shareUrl)}`;
            window.location.href = emailUrl;
        } catch (error) {
            console.error('이메일 공유 오류:', error);
            this.copyShareLink();
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
