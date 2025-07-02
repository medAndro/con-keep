
import { StorageManager } from './storage.js'; // 원래대로 되돌림

// Notification management utility
export class NotificationManager {
    constructor(databaseManager, storageManager) { // storageManager 추가
        this.databaseManager = databaseManager;
        this.storageManager = storageManager; // storageManager 저장
    }

    checkPermission() {
        if ('Notification' in window) {
            return Notification.permission;
        }
        return 'denied';
    }

    async requestPermission() {
        if ('Notification' in window && Notification.permission !== 'granted') {
            return await Notification.requestPermission();
        }
        return Notification.permission;
    }

    startScheduler() {
        // 1시간마다 만료 확인
        setInterval(() => {
            this.checkExpiringCoupons();
        }, 60 * 60 * 1000);

        // 앱 시작시에도 한 번 확인
        setTimeout(() => {
            this.checkExpiringCoupons();
        }, 5000);
    }

    async checkExpiringCoupons() {
        const settings = this.storageManager.getNotificationSettings(); // this.storageManager 사용
        if (!settings.enabled || Notification.permission !== 'granted') {
            return;
        }

        try {
            const coupons = await this.databaseManager.getAllCoupons();

            coupons.forEach(coupon => {
                if (coupon.used || !coupon.expiry) return;

                const daysLeft = this.getDaysUntilExpiry(coupon.expiry);
                
                if ((settings.notify7days && daysLeft === 7) || 
                    (settings.notify1day && daysLeft === 1)) {
                    new Notification(`🌽 ${coupon.brand} 쿠폰 만료 예정`, {
                        body: `${coupon.name} - ${daysLeft}일 후 만료`,
                        icon: '/favicon.ico'
                    });
                }
            });
        } catch (error) {
            console.error('만료 알림 확인 오류:', error);
        }
    }

    getDaysUntilExpiry(expiry) {
        if (!expiry) return 999;
        const today = new Date();
        const expiryDate = new Date(expiry);
        const diffTime = expiryDate - today;
        return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    }
}
