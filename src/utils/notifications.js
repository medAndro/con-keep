
// Notification management utility
export class NotificationManager {
    constructor(databaseManager) {
        this.databaseManager = databaseManager;
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
        // NotificationSettings 컴포넌트에서 설정 가져오기
        const settings = this.getNotificationSettings();
        
        if (!settings.enabled || Notification.permission !== 'granted') {
            return;
        }

        try {
            const coupons = await this.databaseManager.getAllCoupons();

            coupons.forEach(coupon => {
                if (coupon.used || !coupon.expiry) return;

                const daysLeft = this.getDaysUntilExpiry(coupon.expiry);
                
                // 커스텀 날짜 설정과 비교
                if (settings.customDays.includes(daysLeft)) {
                    // 설정된 시간에 맞춰 알림 표시
                    settings.times.forEach(time => {
                        const now = new Date();
                        const [hours, minutes] = time.split(':');
                        const notificationTime = new Date();
                        notificationTime.setHours(parseInt(hours), parseInt(minutes), 0, 0);
                        
                        // 현재 시간과 알림 시간이 비슷하면 (5분 이내) 알림 표시
                        const timeDiff = Math.abs(now - notificationTime);
                        if (timeDiff <= 5 * 60 * 1000) { // 5분 이내
                            new Notification(`🌽 ${coupon.brand} 쿠폰 만료 예정`, {
                                body: `${coupon.name} - ${daysLeft}일 후 만료`,
                                icon: '/favicon.ico',
                                tag: `coupon-${coupon.id}-${daysLeft}` // 중복 방지
                            });
                        }
                    });
                }
            });
        } catch (error) {
            console.error('만료 알림 확인 오류:', error);
        }
    }

    getNotificationSettings() {
        // 기본 설정
        const defaultSettings = {
            enabled: true,
            customDays: [7, 3, 1],
            times: ['09:00']
        };
        
        try {
            const stored = localStorage.getItem('notificationSettings');
            return stored ? { ...defaultSettings, ...JSON.parse(stored) } : defaultSettings;
        } catch (error) {
            console.error('알림 설정 로드 오류:', error);
            return defaultSettings;
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
