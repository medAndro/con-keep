
// Share page component
export class SharePage {
    constructor(app) {
        this.app = app;
    }

    render(coupon) {
        const sharePageHtml = `
            <div id="share-page" class="share-page hidden">
                <div class="share-container">
                    <div class="share-header">
                        <h1>🌽 기프티콘</h1>
                    </div>
                    
                    <div class="coupon-display">
                        <div class="coupon-image-container">
                            <img id="share-coupon-image" src="" alt="쿠폰 이미지" class="coupon-image">
                        </div>
                        
                        <div class="coupon-info">
                            <h2 id="share-coupon-title" class="coupon-title"></h2>
                            <div class="coupon-details">
                                <p><strong>브랜드:</strong> <span id="share-coupon-brand"></span></p>
                                <p><strong>금액:</strong> <span id="share-coupon-amount"></span></p>
                                <p><strong>유효기간:</strong> <span id="share-coupon-expiry"></span></p>
                            </div>
                        </div>
                        
                        <div class="usage-toggle">
                            <label class="toggle-container">
                                <input type="checkbox" id="share-usage-toggle">
                                <span class="toggle-slider"></span>
                                <span class="toggle-label">사용 완료</span>
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        return sharePageHtml;
    }

    show(coupon) {
        document.getElementById('share-coupon-image').src = coupon.imgSrc;
        document.getElementById('share-coupon-title').textContent = coupon.name;
        document.getElementById('share-coupon-brand').textContent = coupon.brand;
        document.getElementById('share-coupon-amount').textContent = coupon.amount ? coupon.amount.toLocaleString() + '원' : '정보 없음';
        document.getElementById('share-coupon-expiry').textContent = coupon.expiry || '정보 없음';
        document.getElementById('share-usage-toggle').checked = coupon.used;
        
        document.getElementById('share-page').classList.remove('hidden');
        document.getElementById('app').style.display = 'none';
    }

    hide() {
        document.getElementById('share-page').classList.add('hidden');
        document.getElementById('app').style.display = 'block';
        
        // URL에서 share 매개변수 제거
        const url = new URL(window.location);
        url.searchParams.delete('share');
        window.history.replaceState({}, document.title, url);
    }
}
