
// 알림 설정 커스터마이징 컴포넌트
export class NotificationSettings {
    constructor() {
        this.settings = this.loadSettings();
    }

    loadSettings() {
        const defaultSettings = {
            enabled: true,
            customDays: [7, 3, 1], // 기본값: 7일, 3일, 1일 전
            times: ['09:00'] // 기본 알림 시간
        };
        
        try {
            const stored = localStorage.getItem('notificationSettings');
            return stored ? { ...defaultSettings, ...JSON.parse(stored) } : defaultSettings;
        } catch (error) {
            console.error('알림 설정 로드 오류:', error);
            return defaultSettings;
        }
    }

    saveSettings() {
        try {
            localStorage.setItem('notificationSettings', JSON.stringify(this.settings));
        } catch (error) {
            console.error('알림 설정 저장 오류:', error);
        }
    }

    createElement() {
        const container = document.createElement('div');
        container.className = 'notification-settings';
        
        container.innerHTML = `
            <div class="setting-group">
                <h3>알림 설정</h3>
                <label class="toggle-container">
                    <input type="checkbox" id="notifications-enabled" ${this.settings.enabled ? 'checked' : ''}>
                    <span class="toggle-slider"></span>
                    <span class="toggle-label">만료 알림 활성화</span>
                </label>
            </div>
            
            <div class="setting-group" id="custom-days-group" style="${this.settings.enabled ? '' : 'display: none;'}">
                <h4>알림 받을 날짜 (만료 전)</h4>
                <div class="days-grid">
                    ${[1, 2, 3, 5, 7, 14, 30].map(day => `
                        <label class="day-option">
                            <input type="checkbox" value="${day}" ${this.settings.customDays.includes(day) ? 'checked' : ''}>
                            <span class="day-label">${day}일 전</span>
                        </label>
                    `).join('')}
                </div>
                <div class="custom-day-input">
                    <input type="number" id="custom-day-value" placeholder="직접 입력" min="1" max="365">
                    <button type="button" id="add-custom-day" class="btn-small">추가</button>
                </div>
            </div>

            <div class="setting-group" id="notification-times" style="${this.settings.enabled ? '' : 'display: none;'}">
                <h4>알림 시간</h4>
                <div class="times-container">
                    ${this.settings.times.map((time, index) => `
                        <div class="time-input-group">
                            <input type="time" value="${time}" data-index="${index}">
                            <button type="button" class="remove-time" data-index="${index}">×</button>
                        </div>
                    `).join('')}
                </div>
                <button type="button" id="add-time" class="btn-small">시간 추가</button>
            </div>
        `;

        this.setupEventListeners(container);
        return container;
    }

    setupEventListeners(container) {
        // 알림 활성화/비활성화
        container.querySelector('#notifications-enabled').addEventListener('change', (e) => {
            this.settings.enabled = e.target.checked;
            const dependentGroups = container.querySelectorAll('#custom-days-group, #notification-times');
            dependentGroups.forEach(group => {
                group.style.display = e.target.checked ? 'block' : 'none';
            });
            this.saveSettings();
        });

        // 날짜 체크박스
        container.querySelectorAll('.days-grid input[type="checkbox"]').forEach(checkbox => {
            checkbox.addEventListener('change', (e) => {
                const day = parseInt(e.target.value);
                if (e.target.checked) {
                    if (!this.settings.customDays.includes(day)) {
                        this.settings.customDays.push(day);
                    }
                } else {
                    this.settings.customDays = this.settings.customDays.filter(d => d !== day);
                }
                this.settings.customDays.sort((a, b) => b - a); // 내림차순 정렬
                this.saveSettings();
            });
        });

        // 커스텀 날짜 추가
        container.querySelector('#add-custom-day').addEventListener('click', () => {
            const input = container.querySelector('#custom-day-value');
            const day = parseInt(input.value);
            if (day && day > 0 && day <= 365 && !this.settings.customDays.includes(day)) {
                this.settings.customDays.push(day);
                this.settings.customDays.sort((a, b) => b - a);
                this.saveSettings();
                input.value = '';
                // 체크박스 갱신
                this.refreshDaysDisplay(container);
            }
        });

        // 시간 관련 이벤트
        this.setupTimeEventListeners(container);
    }

    setupTimeEventListeners(container) {
        // 시간 변경
        container.addEventListener('change', (e) => {
            if (e.target.type === 'time') {
                const index = parseInt(e.target.dataset.index);
                this.settings.times[index] = e.target.value;
                this.saveSettings();
            }
        });

        // 시간 제거
        container.addEventListener('click', (e) => {
            if (e.target.classList.contains('remove-time')) {
                const index = parseInt(e.target.dataset.index);
                this.settings.times.splice(index, 1);
                this.saveSettings();
                this.refreshTimesDisplay(container);
            }
        });

        // 시간 추가
        container.querySelector('#add-time').addEventListener('click', () => {
            this.settings.times.push('09:00');
            this.saveSettings();
            this.refreshTimesDisplay(container);
        });
    }

    refreshDaysDisplay(container) {
        // 날짜 체크박스 갱신 로직
        container.querySelectorAll('.days-grid input[type="checkbox"]').forEach(checkbox => {
            const day = parseInt(checkbox.value);
            checkbox.checked = this.settings.customDays.includes(day);
        });
    }

    refreshTimesDisplay(container) {
        const timesContainer = container.querySelector('.times-container');
        timesContainer.innerHTML = this.settings.times.map((time, index) => `
            <div class="time-input-group">
                <input type="time" value="${time}" data-index="${index}">
                <button type="button" class="remove-time" data-index="${index}">×</button>
            </div>
        `).join('');
    }

    getSettings() {
        return this.settings;
    }
}
