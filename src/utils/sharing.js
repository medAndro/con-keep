
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
        
        // 공유 URL 입력 필드에 설정
        const shareUrlInput = document.getElementById('share-url');
        if (shareUrlInput) {
            shareUrlInput.value = shareUrl;
        }

        // 공유 섹션 표시
        const shareSection = document.getElementById('share-section');
        if (shareSection) {
            shareSection.classList.remove('hidden');
            
            // 소셜 공유 버튼들 생성
            this.createSocialShareButtons(shareUrl, coupon);
        }
        
        this.toastManager.showToast('공유 링크가 생성되었습니다! 🌽🔗', 'success');
    }

    createSocialShareButtons(shareUrl, coupon) {
        const socialContainer = document.getElementById('social-share-buttons');
        if (!socialContainer) return;

        socialContainer.innerHTML = '';
        
        const shareText = `${coupon.brand} ${coupon.name} 기프티콘을 공유합니다!`;
        
        // 카카오톡 공유 버튼
        const kakaoBtn = document.createElement('button');
        kakaoBtn.className = 'social-share-btn kakao-btn';
        kakaoBtn.innerHTML = '📱 카카오톡';
        kakaoBtn.onclick = () => this.shareToKakao(shareUrl, coupon);
        socialContainer.appendChild(kakaoBtn);
        
        // SMS 공유 버튼
        const smsBtn = document.createElement('button');
        smsBtn.className = 'social-share-btn sms-btn';
        smsBtn.innerHTML = '💬 문자';
        smsBtn.onclick = () => this.shareToSMS(shareUrl, shareText);
        socialContainer.appendChild(smsBtn);
        
        // 이메일 공유 버튼
        const emailBtn = document.createElement('button');
        emailBtn.className = 'social-share-btn email-btn';
        emailBtn.innerHTML = '📧 이메일';
        emailBtn.onclick = () => this.shareToEmail(shareUrl, shareText);
        socialContainer.appendChild(emailBtn);
        
        // 네이티브 공유 버튼 (모바일)
        if (navigator.share) {
            const nativeBtn = document.createElement('button');
            nativeBtn.className = 'social-share-btn native-btn';
            nativeBtn.innerHTML = '📤 공유';
            nativeBtn.onclick = () => this.shareNative(shareUrl, shareText);
            socialContainer.appendChild(nativeBtn);
        }
    }

    shareToKakao(url, coupon) {
        if (window.Kakao && window.Kakao.Share) {
            window.Kakao.Share.sendDefault({
                objectType: 'feed',
                content: {
                    title: `${coupon.brand} 기프티콘`,
                    description: coupon.name,
                    imageUrl: coupon.imgSrc,
                    link: {
                        webUrl: url,
                        mobileWebUrl: url
                    }
                },
                buttons: [{
                    title: '기프티콘 보기',
                    link: {
                        webUrl: url,
                        mobileWebUrl: url
                    }
                }]
            });
        } else {
            // 카카오 SDK가 없으면 URL로 공유
            const kakaoUrl = `https://story.kakao.com/share?url=${encodeURIComponent(url)}`;
            window.open(kakaoUrl, '_blank');
        }
    }

    shareToSMS(url, text) {
        const smsUrl = `sms:?body=${encodeURIComponent(text + ' ' + url)}`;
        window.location.href = smsUrl;
    }

    shareToEmail(url, text) {
        const emailUrl = `mailto:?subject=${encodeURIComponent('기프티콘 공유')}&body=${encodeURIComponent(text + '\n\n' + url)}`;
        window.location.href = emailUrl;
    }

    async shareNative(url, text) {
        try {
            await navigator.share({
                title: '기프티콘 공유',
                text: text,
                url: url
            });
        } catch (error) {
            console.log('네이티브 공유 취소됨');
        }
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
