
// Coupon card component
export class CouponCard {
    constructor(coupon, onClickHandler) {
        this.coupon = coupon;
        this.onClickHandler = onClickHandler;
    }

    createElement() {
        const card = document.createElement('div');
        card.className = 'coupon-card';
        card.onclick = () => this.onClickHandler(this.coupon);

        const statusBadge = this.getStatusBadge();
        const expiryBadge = this.getExpiryBadge();

        card.innerHTML = `
            ${statusBadge}
            <img src="${this.coupon.imgSrc}" alt="${this.coupon.name}" class="coupon-image" onerror="this.src='data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjEyMCIgdmlld0JveD0iMCAwIDMwMCAxMjAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIzMDAiIGhlaWdodD0iMTIwIiBmaWxsPSIjRjhGOUZBIi8+Cjx0ZXh0IHg9IjE1MCIgeT0iNjAiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzZDNzU3RCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuydtOuvuOyngCDsl4bsnYw8L3RleHQ+Cjwvc3ZnPg=='">
            <div class="coupon-info">
                <div class="coupon-brand">${this.coupon.brand}</div>
                <div class="coupon-name">${this.coupon.name}</div>
                <div class="coupon-meta">
                    <span class="coupon-amount">${this.coupon.amount ? this.coupon.amount.toLocaleString() + '원' : ''}</span>
                    ${expiryBadge}
                </div>
            </div>
        `;

        return card;
    }

    getStatusBadge() {
        if (this.coupon.used) {
            return '<div class="status-badge used">사용완료</div>';
        }
        if (this.isExpired()) {
            return '<div class="status-badge expired">만료</div>';
        }
        return '';
    }

    getExpiryBadge() {
        if (!this.coupon.expiry) return '';
        
        const daysLeft = this.getDaysUntilExpiry();
        if (daysLeft < 0) {
            return '<span class="expiry-badge expired">만료됨</span>';
        } else if (daysLeft <= 7) {
            return `<span class="expiry-badge urgent">D-${daysLeft}</span>`;
        } else {
            return `<span class="expiry-badge">D-${daysLeft}</span>`;
        }
    }

    getDaysUntilExpiry() {
        if (!this.coupon.expiry) return 999;
        const today = new Date();
        const expiryDate = new Date(this.coupon.expiry);
        const diffTime = expiryDate - today;
        return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    }

    isExpired() {
        if (!this.coupon.expiry) return false;
        return new Date(this.coupon.expiry) < new Date();
    }
}
