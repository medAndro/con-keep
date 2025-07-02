
// Local storage utility functions
export class StorageManager {
    static getApiKey() {
        const encrypted = localStorage.getItem('gemini-api-key');
        return encrypted ? atob(encrypted) : null;
    }

    static setApiKey(apiKey) {
        const encrypted = btoa(apiKey);
        localStorage.setItem('gemini-api-key', encrypted);
    }

    static getTheme() {
        return localStorage.getItem('theme');
    }

    static setTheme(theme) {
        localStorage.setItem('theme', theme);
    }

    static getNotificationSettings() {
        return {
            enabled: localStorage.getItem('notifications-enabled') === 'true',
            notify7days: localStorage.getItem('notify-7days') !== 'false',
            notify1day: localStorage.getItem('notify-1day') !== 'false'
        };
    }

    static setNotificationSettings(settings) {
        localStorage.setItem('notifications-enabled', settings.enabled);
        localStorage.setItem('notification-custom-days', JSON.stringify(settings.customDays));
    }

    static getSortingSettings() {
        return {
            sortBy: localStorage.getItem('sort-by') || 'expiry',
            sortOrder: localStorage.getItem('sort-order') || 'asc'
        };
    }

    static setSortingSettings(settings) {
        localStorage.setItem('sort-by', settings.sortBy);
        localStorage.setItem('sort-order', settings.sortOrder);
    }
}
