// Main application coordinator
import { DatabaseManager } from './utils/database.js';
import { BarcodeScanner } from './utils/scanner.js';
import { AIAnalyzer } from './utils/ai-analyzer.js';
import { StorageManager } from './utils/storage.js';
import { NotificationManager } from './utils/notifications.js';
import { SharingManager } from './utils/sharing.js';
import { ToastManager } from './components/ToastManager.js';
import { CouponCard } from './components/CouponCard.js';
import { ShareModal } from './components/ShareModal.js';

// ConKeep - 옥수수 테마 기프티콘 관리 앱
class ConKeepApp {
    constructor() {
        this.currentCoupon = null;
        this.sharedCoupon = null;
        this.uploadedImageData = null;
        this.currentScanResult = null;
        
        // Initialize managers
        this.toastManager = new ToastManager();
        this.databaseManager = new DatabaseManager();
        this.scanner = new BarcodeScanner(this.toastManager);
        this.aiAnalyzer = new AIAnalyzer(this.toastManager);
        this.notificationManager = new NotificationManager(this.databaseManager, StorageManager);
        this.sharingManager = new SharingManager(this.toastManager);
        this.shareModal = new ShareModal(this.toastManager);
        
        this.init();
    }

    async init() {
        await this.databaseManager.initDB();
        this.setupEventListeners();
        this.setupBroadcastChannel();
        this.loadCoupons();
        this.updateStats();
        this.notificationManager.startScheduler();
        this.checkSharedCoupon();
        
        // 로딩 완료
        setTimeout(() => {
            document.getElementById('loading-screen').style.display = 'none';
            document.getElementById('app').style.display = 'block';
        }, 1500);
    }

    setupEventListeners() {
        // 탭 전환
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const tab = e.currentTarget.dataset.tab;
                this.switchTab(tab);
            });
        });

        // 테마 토글
        document.getElementById('theme-toggle').addEventListener('click', () => {
            this.toggleTheme();
        });

        // 설정 버튼
        document.getElementById('settings-btn').addEventListener('click', () => {
            this.showModal('settings');
        });

        // 파일 업로드
        this.setupFileUpload();
        this.setupMultiFileUpload();
        this.setupMultiFileUpload();

        // 쿠폰 관련 버튼들
        document.getElementById('save-coupon').addEventListener('click', () => {
            this.saveCoupon();
        });

        document.getElementById('cancel-add').addEventListener('click', () => {
            this.resetAddForm();
        });

        document.getElementById('rescan-btn').addEventListener('click', () => {
            this.rescanBarcode();
        });

        // 쿠폰 종류 선택
        document.querySelectorAll('.toggle-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const group = e.target.closest('.toggle-group');
                group.querySelectorAll('.toggle-btn').forEach(b => b.classList.remove('active'));
                e.target.classList.add('active');

                const couponType = e.target.dataset.value;
                const amountGroup = document.getElementById('amount-group');
                const priceGroup = document.getElementById('price-group');

                if (couponType === 'exchange') {
                    amountGroup.classList.add('hidden');
                    priceGroup.classList.remove('hidden');
                } else {
                    amountGroup.classList.remove('hidden');
                    priceGroup.classList.add('hidden');
                }
            });
        });

        // 검색 및 필터
        this.setupSearchAndFilter();

        // 모달 관련
        this.setupModalHandlers();

        // 설정 관련
        this.setupSettingsHandlers();

        // 쿠폰 상세 모달
        this.setupCouponDetailHandlers();

        // 공유 페이지
        this.setupSharingHandlers();

        // 정렬 설정 로드 및 이벤트 리스너
        this.loadSortingSettings();
        document.getElementById('sort-by').addEventListener('change', () => this.filterCoupons());
        document.getElementById('sort-order').addEventListener('change', () => this.filterCoupons());
    }

    loadSortingSettings() {
        const settings = StorageManager.getSortingSettings();
        document.getElementById('sort-by').value = settings.sortBy;
        document.getElementById('sort-order').value = settings.sortOrder;
    }

    saveSortingSettings() {
        const sortBy = document.getElementById('sort-by').value;
        const sortOrder = document.getElementById('sort-order').value;
        StorageManager.setSortingSettings({ sortBy, sortOrder });
    }

    setupFileUpload() {
        const fileInput = document.getElementById('file-input');
        const uploadArea = document.getElementById('upload-area');
        
        console.log('setupFileUpload: Initializing event listeners.');

        uploadArea.addEventListener('dragover', (e) => {
            console.log('setupFileUpload: dragover.');
            e.preventDefault();
            uploadArea.classList.add('dragover');
        });
        
        uploadArea.addEventListener('dragleave', () => {
            console.log('setupFileUpload: dragleave.');
            uploadArea.classList.remove('dragover');
        });
        
        uploadArea.addEventListener('drop', (e) => {
            console.log('setupFileUpload: drop.');
            e.preventDefault();
            uploadArea.classList.remove('dragover');
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                this.handleFileUpload(files[0]);
            }
        });
        
        fileInput.addEventListener('change', (e) => {
            console.log('setupFileUpload: fileInput changed.', e.target.files);
            if (e.target.files.length > 0) {
                this.handleFileUpload(e.target.files[0]);
            }
        });
    }

    setupSearchAndFilter() {
        document.getElementById('search-input').addEventListener('input', () => {
            this.filterCoupons();
        });

        document.getElementById('brand-filter').addEventListener('change', () => {
            this.filterCoupons();
        });

        document.getElementById('status-filter').addEventListener('change', () => {
            this.filterCoupons();
        });
    }

    setupModalHandlers() {
        document.querySelectorAll('.close-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const modal = e.target.dataset.modal;
                this.hideModal(modal);
            });
        });

        document.querySelectorAll('.modal').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    modal.classList.add('hidden');
                }
            });
        });
    }

    setupSettingsHandlers() {
        document.getElementById('save-api-key').addEventListener('click', () => {
            this.saveApiKey();
        });

        document.getElementById('add-notification-day').addEventListener('click', () => {
            this.addNotificationDayInput();
        });

        document.getElementById('export-data').addEventListener('click', () => {
            this.exportData();
        });

        document.getElementById('clear-data').addEventListener('click', () => {
            this.clearAllData();
        });
    }

    setupCouponDetailHandlers() {
        document.getElementById('toggle-used').addEventListener('click', () => {
            this.toggleCouponUsed();
        });

        document.getElementById('share-coupon').addEventListener('click', () => {
            this.shareCoupon();
        });

        document.getElementById('delete-coupon').addEventListener('click', () => {
            this.deleteCoupon();
        });
    }

    setupSharingHandlers() {
        // 앱으로 돌아가기 버튼 핸들러 제거 (공유 페이지에서는 불필요)
        const backButton = document.getElementById('back-to-app');
        if (backButton) {
            backButton.style.display = 'none'; // 버튼 숨기기
        }

        document.getElementById('share-usage-toggle').addEventListener('change', (e) => {
            this.updateSharedCouponUsage(e.target.checked);
        });
    }

    setupBroadcastChannel() {
        this.channel = new BroadcastChannel('coupon-sync');
        this.channel.addEventListener('message', (event) => {
            if (event.data.type === 'coupon-updated') {
                this.loadCoupons();
                this.updateStats();
            }
        });
    }

    switchTab(tabName) {
        // 탭 버튼 활성화
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');

        // 탭 콘텐츠 표시
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(`${tabName}-tab`).classList.add('active');

        // 등록 탭일 때 폼 초기화
        if (tabName === 'add') {
            this.resetAddForm();
        }
    }

    toggleTheme() {
        const body = document.body;
        const themeIcon = document.querySelector('.theme-icon');
        
        if (body.getAttribute('data-theme') === 'dark') {
            body.removeAttribute('data-theme');
            themeIcon.textContent = '🌙';
            StorageManager.setTheme('light');
        } else {
            body.setAttribute('data-theme', 'dark');
            themeIcon.textContent = '☀️';
            StorageManager.setTheme('dark');
        }
    }

    showModal(modalName) {
        const modal = document.getElementById(`${modalName}-modal`);
        modal.classList.remove('hidden');
        
        if (modalName === 'settings') {
            this.loadSettings();
        }
    }

    hideModal(modalName) {
        const modal = document.getElementById(`${modalName}-modal`);
        modal.classList.add('hidden');
    }

    resetAddForm() {
        // 모든 상태 완전히 초기화
        this.uploadedImageData = null;
        this.currentScanResult = null;
        this.currentCoupon = null;
        
        // UI 요소 초기화
        const previewSection = document.getElementById('preview-section');
        const aiAnalysis = document.getElementById('ai-analysis');
        const uploadArea = document.getElementById('upload-area');
        const fileInput = document.getElementById('file-input');
        const scanOverlay = document.getElementById('scan-overlay');
        
        if (previewSection) previewSection.classList.add('hidden');
        if (aiAnalysis) aiAnalysis.classList.add('hidden');
        if (uploadArea) uploadArea.classList.remove('hidden');
        if (scanOverlay) scanOverlay.classList.add('hidden');
        
        // 폼 필드 완전 초기화
        const fields = [
            'coupon-code', 'coupon-brand', 'coupon-name', 
            'coupon-amount', 'coupon-expiry', 'coupon-source', 'coupon-price'
        ];
        
        fields.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            if (field) {
                field.value = '';
                field.disabled = false;
            }
        });

        // 쿠폰 종류 초기화
        document.querySelectorAll('#coupon-type-group .toggle-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelector('#coupon-type-group .toggle-btn[data-value="monetary"]').classList.add('active');
        document.getElementById('amount-group').classList.remove('hidden');
        document.getElementById('price-group').classList.add('hidden');
        
        // 이미지 초기화
        const previewImage = document.getElementById('preview-image');
        if (previewImage) {
            previewImage.src = '';
            previewImage.onload = null;
            previewImage.onerror = null;
        }
        
        // 파일 입력 초기화
        if (fileInput) {
            fileInput.value = '';
        }
        
        console.log('등록 폼 완전 초기화 완료');
        this.toastManager.showToast('등록 폼이 초기화되었습니다 🌽', 'success');
    }

    async handleFileUpload(file) {
        if (!file || !file.type.startsWith('image/')) {
            this.toastManager.showToast('이미지 파일만 업로드 가능합니다 🌽', 'error');
            return;
        }

        // 이전 상태 완전 초기화
        this.uploadedImageData = null;
        this.currentScanResult = null;

        try {
            const reader = new FileReader();
            reader.onload = async (e) => {
                try {
                    this.uploadedImageData = e.target.result;
                    
                    // 이미지 미리보기 표시
                    const previewImage = document.getElementById('preview-image');
                    const uploadArea = document.getElementById('upload-area');
                    const previewSection = document.getElementById('preview-section');
                    
                    if (previewImage && uploadArea && previewSection) {
                        previewImage.src = this.uploadedImageData;
                        uploadArea.classList.add('hidden');
                        previewSection.classList.remove('hidden');
                    }
                    
                    console.log('이미지 업로드 완료, 바코드 스캔 시작');
                    
                    // 바코드 스캔 시작
                    const scanResult = await this.scanner.scanBarcode(this.uploadedImageData);
                    if (scanResult) {
                        this.currentScanResult = scanResult;
                        const codeField = document.getElementById('coupon-code');
                        if (codeField) {
                            codeField.value = scanResult;
                        }
                        console.log('바코드 스캔 성공:', scanResult);
                    }
                    
                    console.log('AI 분석 시작');
                    // AI 분석 시작
                    const apiKey = StorageManager.getApiKey();
                    const aiResult = await this.aiAnalyzer.analyzeImage(this.uploadedImageData, apiKey);
                    if (aiResult) {
                        console.log('AI 분석 결과:', aiResult);
                        this.fillCouponForm(aiResult);
                    }
                    
                } catch (error) {
                    console.error('파일 처리 오류:', error);
                    this.toastManager.showToast('파일 처리 중 오류가 발생했습니다 🌽', 'error');
                }
            };
            
            reader.onerror = () => {
                console.error('파일 읽기 오류');
                this.toastManager.showToast('파일을 읽을 수 없습니다 🌽', 'error');
            };
            
            reader.readAsDataURL(file);
            
        } catch (error) {
            console.error('파일 업로드 오류:', error);
            this.toastManager.showToast('파일 업로드에 실패했습니다 🌽', 'error');
        }
    }

    async rescanBarcode() {
        if (this.uploadedImageData) {
            const scanResult = await this.scanner.scanBarcode(this.uploadedImageData);
            if (scanResult) {
                this.currentScanResult = scanResult;
                document.getElementById('coupon-code').value = scanResult;
            }
        } else {
            this.toastManager.showToast('스캔할 이미지가 없습니다 🌽', 'error');
        }
    }

    fillCouponForm(couponInfo) {
        if (couponInfo.brand) {
            document.getElementById('coupon-brand').value = couponInfo.brand;
        }
        if (couponInfo.name) {
            document.getElementById('coupon-name').value = couponInfo.name;
        }
        if (couponInfo.amount) {
            document.getElementById('coupon-amount').value = couponInfo.amount;
        }
        if (couponInfo.expiry) {
            document.getElementById('coupon-expiry').value = couponInfo.expiry;
        }

        // AI 분석 결과를 기반으로 쿠폰 종류 자동 선택
        const couponTypeGroup = document.getElementById('coupon-type-group');
        const monetaryBtn = couponTypeGroup.querySelector('[data-value="monetary"]');
        const exchangeBtn = couponTypeGroup.querySelector('[data-value="exchange"]');
        const amountGroup = document.getElementById('amount-group');
        const priceGroup = document.getElementById('price-group');

        // 모든 토글 버튼의 active 클래스 제거
        couponTypeGroup.querySelectorAll('.toggle-btn').forEach(btn => btn.classList.remove('active'));

        if (couponInfo.amount === null || couponInfo.amount === 0) {
            // 금액이 없거나 0이면 교환권으로 판단
            exchangeBtn.classList.add('active');
            amountGroup.classList.add('hidden');
            priceGroup.classList.remove('hidden');
        } else {
            // 금액이 있으면 금액권으로 판단
            monetaryBtn.classList.add('active');
            amountGroup.classList.remove('hidden');
            priceGroup.classList.add('hidden');
        }
    }

    async saveCoupon() {
        const code = document.getElementById('coupon-code').value.trim();
        const brand = document.getElementById('coupon-brand').value.trim();
        const name = document.getElementById('coupon-name').value.trim();
        const couponType = document.querySelector('#coupon-type-group .toggle-btn.active').dataset.value;
        const amountInput = document.getElementById('coupon-amount');
        const amount = couponType === 'monetary' ? amountInput.value : null;
        const expiry = document.getElementById('coupon-expiry').value;
        const source = document.getElementById('coupon-source').value.trim();
        const price = document.getElementById('coupon-price').value;

        if (!code || !brand || !name || !expiry || (couponType === 'monetary' && !amount)) {
            this.toastManager.showToast('필수 항목을 입력해주세요 🌽', 'error');
            return;
        }

        // 중복 확인
        if (await this.databaseManager.isDuplicateCoupon(code)) {
            this.toastManager.showToast('이미 등록된 쿠폰입니다 🌽', 'warning');
            return;
        }

        const coupon = {
            id: this.generateId(),
            imgSrc: this.uploadedImageData,
            code: code,
            brand: brand,
            name: name,
            couponType: couponType,
            amount: amount ? parseInt(amount) : null,
            price: price ? parseInt(price) : null,
            expiry: expiry,
            source: source, // 구매처/선물한 사람 정보 추가
            used: false,
            shared: false,
            createdAt: Date.now(),
            updatedAt: Date.now()
        };

        try {
            await this.databaseManager.addCoupon(coupon);
            this.toastManager.showToast('기프티콘이 등록되었습니다! 🌽✨', 'success');
            this.resetAddForm();
            this.switchTab('dashboard');
            this.loadCoupons();
            this.updateStats();
        } catch (error) {
            console.error('쿠폰 저장 오류:', error);
            this.toastManager.showToast('저장에 실패했습니다 🌽', 'error');
        }
    }

    async loadCoupons() {
        try {
            const coupons = await this.databaseManager.getAllCoupons();
            console.log('쿠폰 로드 완료:', coupons.length, '개');
            this.filterCoupons(); // Call filterCoupons to display and sort
            this.updateBrandFilter(coupons);
        } catch (error) {
            console.error('쿠폰 로드 오류:', error);
            this.toastManager.showToast('쿠폰 목록을 불러올 수 없습니다 🌽', 'error');
        }
    }

    displayCoupons(coupons) {
        const grid = document.getElementById('coupons-grid');
        if (!grid) return;

        // 기존 쿠폰 카드들을 모두 제거
        const existingCards = grid.querySelectorAll('.coupon-card');
        existingCards.forEach(card => card.remove());

        const emptyState = document.getElementById('empty-state');

        if (coupons.length === 0) {
            if (emptyState && !grid.contains(emptyState)) {
                grid.appendChild(emptyState);
            }
            if (emptyState) {
                emptyState.style.display = 'block';
            }
            return;
        }

        if (emptyState) {
            emptyState.style.display = 'none';
        }

        

        coupons.forEach(coupon => {
            const couponCard = new CouponCard(coupon, (c) => this.showCouponDetail(c));
            const cardElement = couponCard.createElement();
            grid.appendChild(cardElement);
        });

        console.log('쿠폰 목록 표시 완료:', coupons.length, '개');
    }

    updateBrandFilter(coupons) {
        const brandFilter = document.getElementById('brand-filter');
        const brands = [...new Set(coupons.map(c => c.brand))].sort();
        
        brandFilter.innerHTML = '<option value="">전체</option>';
        brands.forEach(brand => {
            const option = document.createElement('option');
            option.value = brand;
            option.textContent = brand;
            brandFilter.appendChild(option);
        });
    }

    async filterCoupons() {
        const searchTerm = document.getElementById('search-input').value.toLowerCase();
        const brandFilter = document.getElementById('brand-filter').value;
        const statusFilter = document.getElementById('status-filter').value;

        const allCoupons = await this.databaseManager.getAllCoupons();
        
        const filteredCoupons = allCoupons.filter(coupon => {
            const matchesSearch = coupon.brand.toLowerCase().includes(searchTerm) || 
                                coupon.name.toLowerCase().includes(searchTerm);
            const matchesBrand = !brandFilter || coupon.brand === brandFilter;
            
            let matchesStatus = true;
            if (statusFilter === 'used') {
                matchesStatus = coupon.used;
            } else if (statusFilter === 'unused') {
                matchesStatus = !coupon.used && !this.isExpired(coupon.expiry);
            } else if (statusFilter === 'expired') {
                matchesStatus = this.isExpired(coupon.expiry);
            }

            return matchesSearch && matchesBrand && matchesStatus;
        });

        // Apply sorting
        const sortBy = document.getElementById('sort-by').value;
        const sortOrder = document.getElementById('sort-order').value;

        filteredCoupons.sort((a, b) => {
            let compareValue = 0;
            if (sortBy === 'expiry') {
                const dateA = a.expiry ? new Date(a.expiry).getTime() : Infinity;
                const dateB = b.expiry ? new Date(b.expiry).getTime() : Infinity;
                compareValue = dateA - dateB;
            } else if (sortBy === 'createdAt') {
                compareValue = a.createdAt - b.createdAt;
            }

            return sortOrder === 'asc' ? compareValue : -compareValue;
        });

        this.displayCoupons(filteredCoupons);
        this.saveSortingSettings();
    }

    async updateStats() {
        try {
            const coupons = await this.databaseManager.getAllCoupons();
            const totalCount = coupons.length;
            const unusedCount = coupons.filter(c => !c.used && !this.isExpired(c.expiry)).length;
            const expiringCount = coupons.filter(c => !c.used && this.getDaysUntilExpiry(c.expiry) <= 7 && this.getDaysUntilExpiry(c.expiry) >= 0).length;

            document.getElementById('total-count').textContent = totalCount;
            document.getElementById('unused-count').textContent = unusedCount;
            document.getElementById('expiring-count').textContent = expiringCount;
        } catch (error) {
            console.error('통계 업데이트 오류:', error);
        }
    }

    showCouponDetail(coupon) {
        this.currentCoupon = coupon;
        
        document.getElementById('modal-coupon-title').textContent = coupon.name;
        document.getElementById('modal-coupon-image').src = coupon.imgSrc;
        document.getElementById('modal-coupon-brand').textContent = coupon.brand;
        document.getElementById('modal-coupon-name').textContent = coupon.name;
        document.getElementById('modal-coupon-amount').textContent = coupon.amount ? coupon.amount.toLocaleString() + '원' : '정보 없음';
        document.getElementById('modal-coupon-price').textContent = coupon.price ? coupon.price.toLocaleString() + '원' : '정보 없음';
        document.getElementById('modal-coupon-expiry').textContent = coupon.expiry || '정보 없음';
        document.getElementById('modal-coupon-code').textContent = coupon.code;
        document.getElementById('modal-coupon-source').textContent = coupon.source || '정보 없음';
        
        const toggleBtn = document.getElementById('toggle-used');
        toggleBtn.textContent = coupon.used ? '사용 취소' : '사용 완료';
        toggleBtn.className = coupon.used ? 'secondary-btn corn-secondary' : 'primary-btn corn-primary';
        
        this.showModal('coupon');
    }

    async toggleCouponUsed() {
        if (!this.currentCoupon) return;

        try {
            this.currentCoupon.used = !this.currentCoupon.used;
            this.currentCoupon.updatedAt = Date.now();
            
            await this.databaseManager.updateCoupon(this.currentCoupon);
            
            const toggleBtn = document.getElementById('toggle-used');
            toggleBtn.textContent = this.currentCoupon.used ? '사용 취소' : '사용 완료';
            toggleBtn.className = this.currentCoupon.used ? 'secondary-btn corn-secondary' : 'primary-btn corn-primary';
            
            this.toastManager.showToast(this.currentCoupon.used ? '사용 완료로 변경되었습니다 🌽' : '사용 취소되었습니다 🌽', 'success');
            
            this.loadCoupons();
            this.updateStats();
            
            // BroadcastChannel로 다른 탭에 알림
            this.channel.postMessage({
                type: 'coupon-updated',
                couponId: this.currentCoupon.id,
                used: this.currentCoupon.used
            });
            
        } catch (error) {
            console.error('쿠폰 상태 업데이트 오류:', error);
            this.toastManager.showToast('상태 변경에 실패했습니다 🌽', 'error');
        }
    }

    async deleteCoupon() {
        if (!this.currentCoupon) return;

        if (!confirm('정말로 이 쿠폰을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다! 🌽⚠️')) return;

        try {
            await this.databaseManager.deleteCoupon(this.currentCoupon.id);
            this.toastManager.showToast('쿠폰이 삭제되었습니다 🌽', 'success');
            this.hideModal('coupon');
            
            // 즉시 목록과 통계 새로고침
            await this.loadCoupons();
            await this.updateStats();
            
            // BroadcastChannel로 다른 탭에 알림
            this.channel.postMessage({
                type: 'coupon-deleted',
                couponId: this.currentCoupon.id
            });
            
        } catch (error) {
            console.error('쿠폰 삭제 오류:', error);
            this.toastManager.showToast('삭제에 실패했습니다 🌽', 'error');
        }
    }

    shareCoupon() {
        if (!this.currentCoupon) return;
        
        // 공유 쿠폰 저장
        const shareId = this.sharingManager.shareCoupon(this.currentCoupon);
        
        // 새로운 공유 모달 표시
        this.shareModal.show(this.currentCoupon);
    }

    checkSharedCoupon() {
        const urlParams = new URLSearchParams(window.location.search);
        const shareId = urlParams.get('share');
        
        if (shareId) {
            this.loadSharedCoupon(shareId);
        }
    }

    async loadSharedCoupon(shareId) {
        const sharedCoupon = this.sharingManager.getSharedCoupon(shareId);
        
        if (sharedCoupon) {
            this.displaySharedCoupon(sharedCoupon);
        } else {
            const coupon = await this.databaseManager.getCouponById(shareId);
            if (coupon) {
                this.displaySharedCoupon(coupon);
            } else {
                this.toastManager.showToast('공유된 쿠폰을 찾을 수 없습니다 🌽', 'error');
            }
        }
    }

    displaySharedCoupon(coupon) {
        this.sharedCoupon = coupon;
        
        document.getElementById('share-coupon-image').src = coupon.imgSrc;
        document.getElementById('share-coupon-title').textContent = coupon.name;
        document.getElementById('share-coupon-brand').textContent = coupon.brand;
        document.getElementById('share-coupon-amount').textContent = coupon.amount ? coupon.amount.toLocaleString() + '원' : '정보 없음';
        document.getElementById('share-coupon-expiry').textContent = coupon.expiry || '정보 없음';
        document.getElementById('share-usage-toggle').checked = coupon.used;
        
        // 앱으로 돌아가기 버튼 숨기기
        const backButton = document.getElementById('back-to-app');
        if (backButton) {
            backButton.style.display = 'none';
        }
        
        document.getElementById('share-page').classList.remove('hidden');
        document.getElementById('app').style.display = 'none';
    }

    hideSharePage() {
        document.getElementById('share-page').classList.add('hidden');
        document.getElementById('app').style.display = 'block';
        
        const url = new URL(window.location);
        url.searchParams.delete('share');
        window.history.replaceState({}, document.title, url);
    }

    async updateSharedCouponUsage(used) {
        if (!this.sharedCoupon) return;

        try {
            const localCoupon = await this.databaseManager.getCouponById(this.sharedCoupon.id);
            if (localCoupon) {
                localCoupon.used = used;
                localCoupon.updatedAt = Date.now();
                await this.databaseManager.updateCoupon(localCoupon);
                
                this.channel.postMessage({
                    type: 'coupon-updated',
                    couponId: localCoupon.id,
                    used: used
                });
            }

            this.sharedCoupon.used = used;
            this.sharedCoupon.updatedAt = Date.now();
            this.sharingManager.updateSharedCoupon(this.sharedCoupon.id, this.sharedCoupon);
            
            this.toastManager.showToast(used ? '사용 완료로 변경되었습니다 🌽' : '사용 취소되었습니다 🌽', 'success');
            
        } catch (error) {
            console.error('공유 쿠폰 상태 업데이트 오류:', error);
            this.toastManager.showToast('상태 변경에 실패했습니다 🌽', 'error');
        }
    }

    loadSettings() {
        const apiKeyInput = document.getElementById('api-key-input');
        const apiKey = StorageManager.getApiKey();
        if (apiKey) {
            apiKeyInput.value = '**'.repeat(10);
        }
        
        const settings = StorageManager.getNotificationSettings();
        document.getElementById('notifications-enabled').checked = settings.enabled;
        
        this.renderNotificationDays(settings.customDays);
    }

    renderNotificationDays(days) {
        const container = document.getElementById('notification-days-container');
        container.innerHTML = ''; // Clear existing
        days.sort((a, b) => a - b).forEach(day => {
            this.addNotificationDayInput(day);
        });
    }

    addNotificationDayInput(value = '') {
        const container = document.getElementById('notification-days-container');
        const wrapper = document.createElement('div');
        wrapper.className = 'flex items-center space-x-2 mb-2';
        wrapper.innerHTML = `
            <input type="number" class="form-group-input notification-day-input" value="${value}" min="1" placeholder="일">
            <span>일 전</span>
            <button type="button" class="secondary-btn remove-notification-day">삭제</button>
        `;
        container.appendChild(wrapper);

        wrapper.querySelector('.remove-notification-day').addEventListener('click', (e) => {
            wrapper.remove();
        });
    }

    getNotificationDaysFromUI() {
        const container = document.getElementById('notification-days-container');
        const inputs = container.querySelectorAll('input[type="number"]');
        const days = [];
        inputs.forEach(input => {
            const day = parseInt(input.value, 10);
            if (!isNaN(day) && day > 0) {
                days.push(day);
            }
        });
        return [...new Set(days)].sort((a, b) => a - b); // Remove duplicates and sort
    }

    saveApiKey() {
        const apiKeyInput = document.getElementById('api-key-input');
        const apiKey = apiKeyInput.value.trim();
        
        if (apiKey && apiKey !== '**'.repeat(10)) {
            StorageManager.setApiKey(apiKey);
            this.toastManager.showToast('API 키가 저장되었습니다 🌽🤖', 'success');
        }
        
        const settings = {
            enabled: document.getElementById('notifications-enabled').checked,
            customDays: this.getNotificationDaysFromUI()
        };
        
        StorageManager.setNotificationSettings(settings);
        
        if (settings.enabled) {
            this.notificationManager.requestPermission();
        }
    }

    async exportData() {
        try {
            const coupons = await this.databaseManager.getAllCoupons();
            const dataStr = JSON.stringify(coupons, null, 2);
            const dataBlob = new Blob([dataStr], { type: 'application/json' });
            
            const link = document.createElement('a');
            link.href = URL.createObjectURL(dataBlob);
            link.download = `conkeep-backup-${new Date().toISOString().split('T')[0]}.json`;
            link.click();
            
            this.toastManager.showToast('데이터가 내보내졌습니다 🌽💾', 'success');
        } catch (error) {
            console.error('데이터 내보내기 오류:', error);
            this.toastManager.showToast('내보내기에 실패했습니다 🌽', 'error');
        }
    }

    async clearAllData() {
        if (!confirm('정말로 모든 데이터를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다! 🌽⚠️')) {
            return;
        }

        try {
            await this.databaseManager.clearAllCoupons();
            
            this.loadCoupons();
            this.updateStats();
            this.hideModal('settings');
            this.toastManager.showToast('모든 데이터가 삭제되었습니다 🌽', 'success');
        } catch (error) {
            console.error('데이터 삭제 오류:', error);
            this.toastManager.showToast('삭제에 실패했습니다 🌽', 'error');
        }
    }

    getDaysUntilExpiry(expiry) {
        if (!expiry) return 999;
        const today = new Date();
        const expiryDate = new Date(expiry);
        const diffTime = expiryDate - today;
        return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    }

    isExpired(expiry) {
        if (!expiry) return false;
        return new Date(expiry) < new Date();
    }

    generateId() {
        return Date.now().toString(36) + Math.random().toString(36).substr(2);
    }

    // --- 다중 등록 관련 메서드 ---

    setupMultiFileUpload() {
        const multiFileInput = document.getElementById('multi-file-input');
        const multiUploadArea = document.getElementById('multi-upload-area');
        
        console.log('setupMultiFileUpload: Initializing event listeners.');

        multiUploadArea.addEventListener('dragover', (e) => {
            console.log('setupMultiFileUpload: dragover.');
            e.preventDefault();
            multiUploadArea.classList.add('dragover');
        });
        
        multiUploadArea.addEventListener('dragleave', () => {
            console.log('setupMultiFileUpload: dragleave.');
            multiUploadArea.classList.remove('dragover');
        });
        
        multiUploadArea.addEventListener('drop', (e) => {
            console.log('setupMultiFileUpload: drop.');
            e.preventDefault();
            multiUploadArea.classList.remove('dragover');
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                this.handleMultiFileUpload(files);
            }
        });
        
        multiFileInput.addEventListener('change', (e) => {
            console.log('setupMultiFileUpload: multiFileInput changed.', e.target.files);
            if (e.target.files.length > 0) {
                this.handleMultiFileUpload(e.target.files);
            }
        });
    }

    async handleMultiFileUpload(files) {
        const imageFiles = Array.from(files).filter(file => file.type.startsWith('image/'));
        if (imageFiles.length === 0) {
            this.toastManager.showToast('이미지 파일만 업로드 가능합니다 🌽', 'error');
            return;
        }

        this.resetMultiAddForm();
        document.getElementById('multi-progress-section').classList.remove('hidden');
        document.getElementById('multi-results-section').classList.remove('hidden');

        const totalFiles = imageFiles.length;
        let processedCount = 0;
        const resultsGrid = document.getElementById('multi-results-grid');

        for (const file of imageFiles) {
            processedCount++;
            this.updateMultiProgress(processedCount, totalFiles);

            const resultCard = this.createResultCard(file.name);
            resultsGrid.appendChild(resultCard);

            try {
                const imageData = await this.readFileAsDataURL(file);
                const scanResult = await this.scanner.scanBarcode(imageData);

                if (!scanResult) {
                    this.updateResultCard(resultCard, '실패', '바코드를 인식할 수 없습니다.');
                    continue;
                }

                if (await this.databaseManager.isDuplicateCoupon(scanResult)) {
                    this.updateResultCard(resultCard, '실패', '이미 등록된 쿠폰입니다.');
                    continue;
                }

                const apiKey = StorageManager.getApiKey();
                const aiResult = await this.aiAnalyzer.analyzeImage(imageData, apiKey);

                if (!aiResult || !aiResult.brand || !aiResult.name || !aiResult.expiry) {
                    this.updateResultCard(resultCard, '실패', 'AI가 쿠폰 정보를 분석하지 못했습니다.');
                    continue;
                }

                const coupon = {
                    id: this.generateId(),
                    imgSrc: imageData,
                    code: scanResult,
                    brand: aiResult.brand,
                    name: aiResult.name,
                    couponType: (aiResult.amount === null || aiResult.amount === 0) ? 'exchange' : 'monetary',
                    amount: aiResult.amount ? parseInt(aiResult.amount) : null,
                    price: aiResult.price ? parseInt(aiResult.price) : null,
                    expiry: aiResult.expiry,
                    source: '다중 등록',
                    used: false,
                    shared: false,
                    createdAt: Date.now(),
                    updatedAt: Date.now()
                };

                await this.databaseManager.addCoupon(coupon);
                this.updateResultCard(resultCard, '성공', `${coupon.brand} - ${coupon.name}`);

            } catch (error) {
                console.error('다중 등록 처리 오류:', error);
                this.updateResultCard(resultCard, '실패', '처리 중 오류가 발생했습니다.');
            }
        }

        this.toastManager.showToast(`총 ${totalFiles}개의 파일 중 ${resultsGrid.querySelectorAll('.success').length}개가 등록되었습니다.`, 'success');
        this.loadCoupons();
        this.updateStats();
    }

    readFileAsDataURL(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = () => reject(reader.error);
            reader.readAsDataURL(file);
        });
    }

    updateMultiProgress(current, total) {
        const progressBar = document.getElementById('multi-progress-bar');
        const progressText = document.getElementById('multi-progress-text');
        const percentage = (current / total) * 100;
        progressBar.style.width = `${percentage}%`;
        progressText.textContent = `${current}/${total} 처리 중...`;
    }

    createResultCard(fileName) {
        const card = document.createElement('div');
        card.className = 'result-card';
        card.innerHTML = `
            <div class="file-name">${fileName}</div>
            <div class="status">처리 중...</div>
            <div class="message"></div>
        `;
        return card;
    }

    updateResultCard(card, status, message) {
        card.querySelector('.status').textContent = status;
        card.querySelector('.message').textContent = message;
        card.classList.add(status === '성공' ? 'success' : 'error');
    }

    resetMultiAddForm() {
        document.getElementById('multi-progress-section').classList.add('hidden');
        document.getElementById('multi-results-section').classList.add('hidden');
        document.getElementById('multi-results-grid').innerHTML = '';
        document.getElementById('multi-file-input').value = '';
        this.updateMultiProgress(0, 0);
    }
}

// 앱 초기화
document.addEventListener('DOMContentLoaded', () => {
    window.conKeepApp = new ConKeepApp();
    
    // 초기 테마 설정
    const savedTheme = StorageManager.getTheme();
    if (savedTheme === 'dark') {
        document.body.setAttribute('data-theme', 'dark');
        document.querySelector('.theme-icon').textContent = '☀️';
    }
    
    console.log('ConKeep 앱 초기화 완료');
});

// 전역 함수 (HTML에서 호출용)
function switchTab(tabName) {
    if (window.conKeepApp) {
        window.conKeepApp.switchTab(tabName);
    }
}
