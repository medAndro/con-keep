
// 쿠폰 카테고리 선택 컴포넌트
export class CouponCategorySelector {
    constructor(onCategoryChange) {
        this.onCategoryChange = onCategoryChange;
        this.selectedCategory = 'amount'; // 기본값: 금액권
    }

    createElement() {
        const container = document.createElement('div');
        container.className = 'coupon-category-selector mb-4';
        
        container.innerHTML = `
            <label class="block text-sm font-medium mb-2">쿠폰 유형</label>
            <div class="category-options">
                <label class="category-option">
                    <input type="radio" name="coupon-category" value="amount" checked>
                    <span class="category-label">
                        <div class="category-icon">💰</div>
                        <div class="category-text">
                            <div class="category-title">금액권</div>
                            <div class="category-desc">금액이 설정된 쿠폰</div>
                        </div>
                    </span>
                </label>
                <label class="category-option">
                    <input type="radio" name="coupon-category" value="exchange">
                    <span class="category-label">
                        <div class="category-icon">🎁</div>
                        <div class="category-text">
                            <div class="category-title">교환권</div>
                            <div class="category-desc">상품/서비스 교환 쿠폰</div>
                        </div>
                    </span>
                </label>
            </div>
        `;

        // 이벤트 리스너 추가
        const radioButtons = container.querySelectorAll('input[name="coupon-category"]');
        radioButtons.forEach(radio => {
            radio.addEventListener('change', (e) => {
                this.selectedCategory = e.target.value;
                this.updateAmountField();
                if (this.onCategoryChange) {
                    this.onCategoryChange(this.selectedCategory);
                }
            });
        });

        return container;
    }

    updateAmountField() {
        const amountField = document.getElementById('coupon-amount');
        const amountLabel = document.querySelector('label[for="coupon-amount"]');
        
        if (this.selectedCategory === 'amount') {
            amountField.disabled = false;
            amountField.placeholder = '금액을 입력하세요';
            amountLabel.textContent = '금액 (원)';
        } else {
            amountField.disabled = true;
            amountField.value = '';
            amountField.placeholder = '교환권은 금액이 없습니다';
            amountLabel.textContent = '금액 (교환권)';
        }
    }

    getSelectedCategory() {
        return this.selectedCategory;
    }
}
