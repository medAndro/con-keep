
// 공유 모달 컴포넌트
export class ShareModal {
    constructor(toastManager) {
        this.toastManager = toastManager;
        this.currentCoupon = null;
        this.modal = null;
        this.createModal();
    }

    createModal() {
        this.modal = document.createElement('div');
        this.modal.id = 'share-modal';
        this.modal.className = 'modal hidden';
        this.modal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h2>🌽 쿠폰 공유하기</h2>
                    <button class="close-btn" data-modal="share">×</button>
                </div>
                <div class="modal-body">
                    <div class="share-url-section">
                        <label>공유 링크</label>
                        <div class="url-input-group">
                            <input type="text" id="share-url-input" readonly>
                            <button id="copy-share-link" class="copy-btn">📋 복사</button>
                        </div>
                    </div>
                    
                    <div class="share-buttons-section">
                        <h3>소셜 공유</h3>
                        <div class="social-share-grid" id="social-share-buttons">
                            <!-- 소셜 공유 버튼들이 여기에 동적으로 추가됩니다 -->
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.appendChild(this.modal);
        this.setupEventListeners();
    }

    setupEventListeners() {
        // 모달 닫기
        this.modal.querySelector('.close-btn').addEventListener('click', () => {
            this.hide();
        });

        this.modal.addEventListener('click', (e) => {
            if (e.target === this.modal) {
                this.hide();
            }
        });

        // 링크 복사
        document.getElementById('copy-share-link').addEventListener('click', () => {
            this.copyShareLink();
        });
    }

    show(coupon) {
        this.currentCoupon = coupon;
        const shareUrl = `${window.location.origin}${window.location.pathname}?share=${coupon.id}`;
        
        // URL 입력 필드에 설정
        document.getElementById('share-url-input').value = shareUrl;
        
        // 소셜 공유 버튼 생성
        this.createSocialShareButtons(shareUrl, coupon);
        
        // 모달 표시
        this.modal.classList.remove('hidden');
        
        this.toastManager.showToast('공유 링크가 생성되었습니다! 🌽🔗', 'success');
    }

    hide() {
        this.modal.classList.add('hidden');
    }

    createSocialShareButtons(shareUrl, coupon) {
        const container = document.getElementById('social-share-buttons');
        container.innerHTML = '';
        
        const shareText = `${coupon.brand} ${coupon.name} 기프티콘을 공유합니다!`;
        
        const buttons = [
            {
                name: '카카오톡',
                icon: '💬',
                class: 'kakao-btn',
                action: () => this.shareToKakao(shareUrl, coupon)
            },
            {
                name: '문자',
                icon: '📱',
                class: 'sms-btn',
                action: () => this.shareToSMS(shareUrl, shareText)
            },
            {
                name: '이메일',
                icon: '📧',
                class: 'email-btn',
                action: () => this.shareToEmail(shareUrl, shareText)
            }
        ];

        // 네이티브 공유 지원 시 추가
        if (navigator.share) {
            buttons.push({
                name: '기타',
                icon: '📤',
                class: 'native-btn',
                action: () => this.shareNative(shareUrl, shareText)
            });
        }

        buttons.forEach(button => {
            const btn = document.createElement('button');
            btn.className = `social-share-btn ${button.class}`;
            btn.innerHTML = `${button.icon} ${button.name}`;
            btn.onclick = button.action;
            container.appendChild(btn);
        });
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
        const shareUrlInput = document.getElementById('share-url-input');
        if (shareUrlInput) {
            shareUrlInput.select();
            shareUrlInput.setSelectionRange(0, 99999);
            
            try {
                document.execCommand('copy');
                this.toastManager.showToast('링크가 복사되었습니다! 🌽📋', 'success');
            } catch (error) {
                console.error('복사 오류:', error);
                this.toastManager.showToast('링크 복사에 실패했습니다 🌽', 'error');
            }
        }
    }
}
