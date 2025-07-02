
// 쿠폰 정렬 컴포넌트
export class CouponSorter {
    constructor(onSortChange) {
        this.onSortChange = onSortChange;
        this.currentSort = this.loadSortSettings();
    }

    loadSortSettings() {
        const defaultSort = {
            field: 'expiry', // expiry, created, brand, amount
            order: 'asc' // asc, desc
        };
        
        try {
            const stored = localStorage.getItem('couponSortSettings');
            return stored ? JSON.parse(stored) : defaultSort;
        } catch (error) {
            console.error('정렬 설정 로드 오류:', error);
            return defaultSort;
        }
    }

    saveSortSettings() {
        try {
            localStorage.setItem('couponSortSettings', JSON.stringify(this.currentSort));
        } catch (error) {
            console.error('정렬 설정 저장 오류:', error);
        }
    }

    createElement() {
        const container = document.createElement('div');
        container.className = 'coupon-sorter';
        
        container.innerHTML = `
            <div class="sort-controls">
                <div class="sort-field">
                    <label for="sort-field">정렬 기준</label>
                    <select id="sort-field">
                        <option value="expiry" ${this.currentSort.field === 'expiry' ? 'selected' : ''}>만료일</option>
                        <option value="created" ${this.currentSort.field === 'created' ? 'selected' : ''}>등록일</option>
                        <option value="brand" ${this.currentSort.field === 'brand' ? 'selected' : ''}>브랜드</option>
                        <option value="amount" ${this.currentSort.field === 'amount' ? 'selected' : ''}>금액</option>
                    </select>
                </div>
                <div class="sort-order">
                    <label for="sort-order">정렬 순서</label>
                    <select id="sort-order">
                        <option value="asc" ${this.currentSort.order === 'asc' ? 'selected' : ''}>오름차순</option>
                        <option value="desc" ${this.currentSort.order === 'desc' ? 'selected' : ''}>내림차순</option>
                    </select>
                </div>
                <button type="button" id="apply-sort" class="btn-sort">적용</button>
            </div>
        `;

        this.setupEventListeners(container);
        return container;
    }

    setupEventListeners(container) {
        const fieldSelect = container.querySelector('#sort-field');
        const orderSelect = container.querySelector('#sort-order');
        const applyButton = container.querySelector('#apply-sort');

        const applySort = () => {
            this.currentSort.field = fieldSelect.value;
            this.currentSort.order = orderSelect.value;
            this.saveSortSettings();
            
            if (this.onSortChange) {
                this.onSortChange(this.currentSort);
            }
        };

        fieldSelect.addEventListener('change', applySort);
        orderSelect.addEventListener('change', applySort);
        applyButton.addEventListener('click', applySort);
    }

    sortCoupons(coupons) {
        return [...coupons].sort((a, b) => {
            let valueA, valueB;

            switch (this.currentSort.field) {
                case 'expiry':
                    valueA = a.expiry ? new Date(a.expiry) : new Date('9999-12-31');
                    valueB = b.expiry ? new Date(b.expiry) : new Date('9999-12-31');
                    break;
                case 'created':
                    valueA = new Date(a.createdAt || 0);
                    valueB = new Date(b.createdAt || 0);
                    break;
                case 'brand':
                    valueA = a.brand.toLowerCase();
                    valueB = b.brand.toLowerCase();
                    break;
                case 'amount':
                    valueA = a.amount || 0;
                    valueB = b.amount || 0;
                    break;
                default:
                    return 0;
            }

            let comparison = 0;
            if (valueA < valueB) comparison = -1;
            if (valueA > valueB) comparison = 1;

            return this.currentSort.order === 'desc' ? -comparison : comparison;
        });
    }

    getCurrentSort() {
        return this.currentSort;
    }
}
