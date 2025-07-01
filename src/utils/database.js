
// Database utility functions
export class DatabaseManager {
    constructor() {
        this.db = null;
    }

    async initDB() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open('ConKeepDB', 1);
            
            request.onerror = () => reject(request.error);
            request.onsuccess = () => {
                this.db = request.result;
                resolve();
            };
            
            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                if (!db.objectStoreNames.contains('coupons')) {
                    const store = db.createObjectStore('coupons', { keyPath: 'id' });
                    store.createIndex('brand', 'brand', { unique: false });
                    store.createIndex('expiry', 'expiry', { unique: false });
                    store.createIndex('used', 'used', { unique: false });
                }
            };
        });
    }

    async addCoupon(coupon) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readwrite');
            const store = transaction.objectStore('coupons');
            const request = store.add(coupon);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async getAllCoupons() {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readonly');
            const store = transaction.objectStore('coupons');
            const request = store.getAll();

            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async getCouponById(id) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readonly');
            const store = transaction.objectStore('coupons');
            const request = store.get(id);

            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async updateCoupon(coupon) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readwrite');
            const store = transaction.objectStore('coupons');
            const request = store.put(coupon);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async deleteCoupon(couponId) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readwrite');
            const store = transaction.objectStore('coupons');
            const request = store.delete(couponId);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async clearAllCoupons() {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readwrite');
            const store = transaction.objectStore('coupons');
            const request = store.clear();

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async isDuplicateCoupon(code) {
        const coupons = await this.getAllCoupons();
        return coupons.some(coupon => coupon.code === code);
    }
}
