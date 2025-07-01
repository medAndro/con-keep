
// Local storage utility functions
export class StorageManager {
    static getApiKey() {
        return localStorage.getItem('gemini-api-key');
    }

    static setApiKey(apiKey) {
        localStorage.setItem('gemini-api-key', apiKey);
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
        localStorage.setItem('notify-7days', settings.notify7days);
        localStorage.setItem('notify-1day', settings.notify1day);
    }
}
