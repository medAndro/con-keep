// ConKeep - 옥수수 테마 기프티콘 관리 앱
class ConKeepApp {
    constructor() {
        this.db = null;
        this.currentCoupon = null;
        this.sharedCoupon = null;
        this.sharedCoupons = new Map(); // 공유 쿠폰 저장소
        this.apiKey = localStorage.getItem('gemini-api-key');
        this.uploadedImageData = null; // 업로드된 이미지 데이터 저장
        this.currentScanResult = null; // 현재 스캔 결과 저장
        
        this.init();
    }

    async init() {
        await this.initDB();
        this.setupEventListeners();
        this.setupBroadcastChannel();
        this.loadCoupons();
        this.updateStats();
        this.checkNotificationPermission();
        this.startNotificationScheduler();
        this.checkSharedCoupon();
        
        // 로딩 완료
        setTimeout(() => {
            document.getElementById('loading-screen').style.display = 'none';
            document.getElementById('app').style.display = 'block';
        }, 1500);
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
        const fileInput = document.getElementById('file-input');
        const uploadArea = document.getElementById('upload-area');
        
        uploadArea.addEventListener('click', () => fileInput.click());
        
        uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.classList.add('dragover');
        });
        
        uploadArea.addEventListener('dragleave', () => {
            uploadArea.classList.remove('dragover');
        });
        
        uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('dragover');
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                this.handleFileUpload(files[0]);
            }
        });
        
        fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.handleFileUpload(e.target.files[0]);
            }
        });

        // 쿠폰 저장
        document.getElementById('save-coupon').addEventListener('click', () => {
            this.saveCoupon();
        });

        // 취소 버튼
        document.getElementById('cancel-add').addEventListener('click', () => {
            this.resetAddForm();
        });

        // 재스캔 버튼
        document.getElementById('rescan-btn').addEventListener('click', () => {
            this.rescanBarcode();
        });

        // 검색 및 필터
        document.getElementById('search-input').addEventListener('input', () => {
            this.filterCoupons();
        });

        document.getElementById('filter-btn').addEventListener('click', () => {
            const panel = document.getElementById('filter-panel');
            panel.classList.toggle('hidden');
        });

        document.getElementById('brand-filter').addEventListener('change', () => {
            this.filterCoupons();
        });

        document.getElementById('status-filter').addEventListener('change', () => {
            this.filterCoupons();
        });

        // 모달 닫기
        document.querySelectorAll('.close-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const modal = e.target.dataset.modal;
                this.hideModal(modal);
            });
        });

        // 모달 바깥 클릭으로 닫기
        document.querySelectorAll('.modal').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    modal.classList.add('hidden');
                }
            });
        });

        // 설정 관련
        document.getElementById('save-api-key').addEventListener('click', () => {
            this.saveApiKey();
        });

        document.getElementById('export-data').addEventListener('click', () => {
            this.exportData();
        });

        document.getElementById('clear-data').addEventListener('click', () => {
            this.clearAllData();
        });

        // 쿠폰 상세 모달
        document.getElementById('toggle-used').addEventListener('click', () => {
            this.toggleCouponUsed();
        });

        document.getElementById('share-coupon').addEventListener('click', () => {
            this.shareCoupon();
        });

        document.getElementById('delete-coupon').addEventListener('click', () => {
            this.deleteCoupon();
        });

        document.getElementById('copy-link').addEventListener('click', () => {
            this.copyShareLink();
        });

        // 공유 페이지
        document.getElementById('back-to-app').addEventListener('click', () => {
            this.hideSharePage();
        });

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
            localStorage.setItem('theme', 'light');
        } else {
            body.setAttribute('data-theme', 'dark');
            themeIcon.textContent = '☀️';
            localStorage.setItem('theme', 'dark');
        }
    }

    // 초기 테마 설정
    initTheme() {
        const savedTheme = localStorage.getItem('theme');
        const themeIcon = document.querySelector('.theme-icon');
        
        if (savedTheme === 'dark') {
            document.body.setAttribute('data-theme', 'dark');
            themeIcon.textContent = '☀️';
        } else {
            themeIcon.textContent = '🌙';
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
        // 모든 상태 초기화
        this.uploadedImageData = null;
        this.currentScanResult = null;
        this.currentCoupon = null;
        
        // UI 요소 초기화
        const previewSection = document.getElementById('preview-section');
        const aiAnalysis = document.getElementById('ai-analysis');
        const uploadArea = document.getElementById('upload-area');
        const fileInput = document.getElementById('file-input');
        
        previewSection.classList.add('hidden');
        aiAnalysis.classList.add('hidden');
        uploadArea.classList.remove('hidden');
        
        // 폼 필드 초기화
        document.getElementById('coupon-code').value = '';
        document.getElementById('coupon-brand').value = '';
        document.getElementById('coupon-name').value = '';
        document.getElementById('coupon-amount').value = '';
        document.getElementById('coupon-expiry').value = '';
        
        // 이미지 초기화
        const previewImage = document.getElementById('preview-image');
        if (previewImage) {
            previewImage.src = '';
        }
        
        // 파일 입력 초기화
        fileInput.value = '';
        
        this.showToast('등록 폼이 초기화되었습니다 🌽', 'success');
    }

    async handleFileUpload(file) {
        if (!file.type.startsWith('image/')) {
            this.showToast('이미지 파일만 업로드 가능합니다 🌽', 'error');
            return;
        }

        try {
            // 파일을 base64로 변환
            const reader = new FileReader();
            reader.onload = async (e) => {
                this.uploadedImageData = e.target.result;
                
                // 이미지 미리보기 표시
                document.getElementById('preview-image').src = this.uploadedImageData;
                document.getElementById('upload-area').classList.add('hidden');
                document.getElementById('preview-section').classList.remove('hidden');
                
                // 바코드 스캔 시작
                await this.scanBarcode(this.uploadedImageData);
            };
            reader.readAsDataURL(file);
            
        } catch (error) {
            console.error('파일 업로드 오류:', error);
            this.showToast('파일 업로드에 실패했습니다 🌽', 'error');
        }
    }

    async scanBarcode(imageSrc) {
        const scanOverlay = document.getElementById('scan-overlay');
        scanOverlay.classList.remove('hidden');
        
        try {
            // 이미지를 Image 객체로 변환
            const img = new Image();
            img.crossOrigin = 'anonymous';
            
            return new Promise((resolve, reject) => {
                img.onload = async () => {
                    try {
                        // Canvas에 그리기
                        const canvas = document.createElement('canvas');
                        const ctx = canvas.getContext('2d');
                        canvas.width = img.width;
                        canvas.height = img.height;
                        ctx.drawImage(img, 0, 0);
                        
                        // ImageData 추출
                        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                        
                        // ZXing으로 바코드 스캔
                        const codeReader = new ZXing.BrowserMultiFormatReader();
                        const result = await codeReader.decodeFromImageData(imageData);
                        
                        this.currentScanResult = result.text;
                        document.getElementById('coupon-code').value = result.text;
                        
                        scanOverlay.classList.add('hidden');
                        this.showToast('바코드 인식 성공! 🌽✨', 'success');
                        
                        // AI 분석 시작
                        await this.analyzeWithAI();
                        
                        resolve(result.text);
                        
                    } catch (error) {
                        console.error('바코드 스캔 오류:', error);
                        scanOverlay.classList.add('hidden');
                        this.showToast('바코드를 인식할 수 없습니다. 수동으로 입력해주세요 🌽', 'warning');
                        resolve(null);
                    }
                };
                
                img.onerror = () => {
                    scanOverlay.classList.add('hidden');
                    this.showToast('이미지 로드에 실패했습니다 🌽', 'error');
                    reject(new Error('이미지 로드 실패'));
                };
                
                img.src = imageSrc;
            });
            
        } catch (error) {
            scanOverlay.classList.add('hidden');
            console.error('스캔 프로세스 오류:', error);
            this.showToast('스캔 중 오류가 발생했습니다 🌽', 'error');
        }
    }

    async rescanBarcode() {
        if (this.uploadedImageData) {
            await this.scanBarcode(this.uploadedImageData);
        } else {
            this.showToast('스캔할 이미지가 없습니다 🌽', 'error');
        }
    }

    async analyzeWithAI() {
        if (!this.apiKey) {
            this.showToast('Gemini API 키가 필요합니다. 설정에서 등록해주세요 🌽', 'warning');
            return;
        }

        if (!this.uploadedImageData) {
            this.showToast('분석할 이미지가 없습니다 🌽', 'error');
            return;
        }

        const aiAnalysis = document.getElementById('ai-analysis');
        aiAnalysis.classList.remove('hidden');

        try {
            // 이미지를 Gemini API 형식으로 변환
            const base64Data = this.uploadedImageData.split(',')[1];
            
            const requestBody = {
                contents: [{
                    parts: [
                        {
                            text: "이 기프티콘 이미지를 분석해서 다음 정보를 JSON 형식으로 추출해주세요:\n" +
                                  "- brand: 브랜드명 (예: 스타벅스, 이디야커피)\n" +
                                  "- name: 상품명 (예: 아메리카노 Tall)\n" +
                                  "- amount: 금액 (숫자만, 예: 4500)\n" +
                                  "- expiry: 유효기간 (YYYY-MM-DD 형식)\n" +
                                  "정보를 찾을 수 없으면 null로 설정해주세요. 응답은 순수 JSON만 해주세요."
                        },
                        {
                            inline_data: {
                                mime_type: "image/jpeg",
                                data: base64Data
                            }
                        }
                    ]
                }]
            };

            const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${this.apiKey}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(`API 오류: ${response.status}`);
            }

            const data = await response.json();
            console.log('Gemini API 응답:', data);
            
            if (data.candidates && data.candidates[0] && data.candidates[0].content) {
                const text = data.candidates[0].content.parts[0].text;
                console.log('추출된 텍스트:', text);
                
                try {
                    // JSON 파싱 시도
                    const jsonMatch = text.match(/\{[\s\S]*\}/);
                    if (jsonMatch) {
                        const couponInfo = JSON.parse(jsonMatch[0]);
                        this.fillCouponForm(couponInfo);
                        this.showToast('AI 분석 완료! 정보를 확인해주세요 🌽🤖', 'success');
                    } else {
                        throw new Error('JSON 형식을 찾을 수 없음');
                    }
                } catch (parseError) {
                    console.error('JSON 파싱 오류:', parseError);
                    this.showToast('AI 분석 결과를 처리할 수 없습니다. 수동으로 입력해주세요 🌽', 'warning');
                }
            } else {
                throw new Error('API 응답이 비어있습니다');
            }

        } catch (error) {
            console.error('AI 분석 오류:', error);
            this.showToast('AI 분석에 실패했습니다. 수동으로 입력해주세요 🌽', 'warning');
        } finally {
            aiAnalysis.classList.add('hidden');
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
    }

    async saveCoupon() {
        const code = document.getElementById('coupon-code').value.trim();
        const brand = document.getElementById('coupon-brand').value.trim();
        const name = document.getElementById('coupon-name').value.trim();
        const amount = document.getElementById('coupon-amount').value;
        const expiry = document.getElementById('coupon-expiry').value;

        if (!code || !brand || !name) {
            this.showToast('필수 항목을 입력해주세요 🌽', 'error');
            return;
        }

        // 중복 확인
        if (await this.isDuplicateCoupon(code)) {
            this.showToast('이미 등록된 쿠폰입니다 🌽', 'warning');
            return;
        }

        const coupon = {
            id: this.generateId(),
            imgSrc: this.uploadedImageData,
            code: code,
            brand: brand,
            name: name,
            amount: amount ? parseInt(amount) : null,
            expiry: expiry,
            used: false,
            shared: false,
            createdAt: Date.now(),
            updatedAt: Date.now()
        };

        try {
            await this.addCouponToDB(coupon);
            this.showToast('기프티콘이 등록되었습니다! 🌽✨', 'success');
            this.resetAddForm();
            this.switchTab('dashboard');
            this.loadCoupons();
            this.updateStats();
        } catch (error) {
            console.error('쿠폰 저장 오류:', error);
            this.showToast('저장에 실패했습니다 🌽', 'error');
        }
    }

    async isDuplicateCoupon(code) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readonly');
            const store = transaction.objectStore('coupons');
            const request = store.getAll();

            request.onsuccess = () => {
                const coupons = request.result;
                const duplicate = coupons.find(coupon => coupon.code === code);
                resolve(!!duplicate);
            };

            request.onerror = () => reject(request.error);
        });
    }

    async addCouponToDB(coupon) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readwrite');
            const store = transaction.objectStore('coupons');
            const request = store.add(coupon);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async loadCoupons() {
        try {
            const coupons = await this.getCouponsFromDB();
            this.displayCoupons(coupons);
            this.updateBrandFilter(coupons);
        } catch (error) {
            console.error('쿠폰 로드 오류:', error);
        }
    }

    async getCouponsFromDB() {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readonly');
            const store = transaction.objectStore('coupons');
            const request = store.getAll();

            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    displayCoupons(coupons) {
        const grid = document.getElementById('coupons-grid');
        const emptyState = document.getElementById('empty-state');

        if (coupons.length === 0) {
            grid.innerHTML = '';
            grid.appendChild(emptyState);
            return;
        }

        emptyState.remove();
        grid.innerHTML = '';

        // 만료일 순으로 정렬
        coupons.sort((a, b) => {
            if (!a.expiry && !b.expiry) return 0;
            if (!a.expiry) return 1;
            if (!b.expiry) return -1;
            return new Date(a.expiry) - new Date(b.expiry);
        });

        coupons.forEach(coupon => {
            const card = this.createCouponCard(coupon);
            grid.appendChild(card);
        });
    }

    createCouponCard(coupon) {
        const card = document.createElement('div');
        card.className = 'coupon-card';
        card.onclick = () => this.showCouponDetail(coupon);

        const statusBadge = this.getStatusBadge(coupon);
        const expiryBadge = this.getExpiryBadge(coupon);

        card.innerHTML = `
            ${statusBadge}
            <img src="${coupon.imgSrc}" alt="${coupon.name}" class="coupon-image" onerror="this.src='data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjEyMCIgdmlld0JveD0iMCAwIDMwMCAxMjAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIzMDAiIGhlaWdodD0iMTIwIiBmaWxsPSIjRjhGOUZBIi8+Cjx0ZXh0IHg9IjE1MCIgeT0iNjAiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzZDNzU3RCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuydtOuvuOyngCDsl4bsnYw8L3RleHQ+Cjwvc3ZnPg=='">
            <div class="coupon-info">
                <div class="coupon-brand">${coupon.brand}</div>
                <div class="coupon-name">${coupon.name}</div>
                <div class="coupon-meta">
                    <span class="coupon-amount">${coupon.amount ? coupon.amount.toLocaleString() + '원' : ''}</span>
                    ${expiryBadge}
                </div>
            </div>
        `;

        return card;
    }

    getStatusBadge(coupon) {
        if (coupon.used) {
            return '<div class="status-badge used">사용완료</div>';
        }
        if (this.isExpired(coupon.expiry)) {
            return '<div class="status-badge expired">만료</div>';
        }
        return '';
    }

    getExpiryBadge(coupon) {
        if (!coupon.expiry) return '';
        
        const daysLeft = this.getDaysUntilExpiry(coupon.expiry);
        if (daysLeft < 0) {
            return '<span class="expiry-badge expired">만료됨</span>';
        } else if (daysLeft <= 7) {
            return `<span class="expiry-badge urgent">D-${daysLeft}</span>`;
        } else {
            return `<span class="expiry-badge">D-${daysLeft}</span>`;
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

        const allCoupons = await this.getCouponsFromDB();
        
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

        this.displayCoupons(filteredCoupons);
    }

    async updateStats() {
        try {
            const coupons = await this.getCouponsFromDB();
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
        document.getElementById('modal-coupon-expiry').textContent = coupon.expiry || '정보 없음';
        document.getElementById('modal-coupon-code').textContent = coupon.code;
        
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
            
            await this.updateCouponInDB(this.currentCoupon);
            
            const toggleBtn = document.getElementById('toggle-used');
            toggleBtn.textContent = this.currentCoupon.used ? '사용 취소' : '사용 완료';
            toggleBtn.className = this.currentCoupon.used ? 'secondary-btn corn-secondary' : 'primary-btn corn-primary';
            
            this.showToast(this.currentCoupon.used ? '사용 완료로 변경되었습니다 🌽' : '사용 취소되었습니다 🌽', 'success');
            
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
            this.showToast('상태 변경에 실패했습니다 🌽', 'error');
        }
    }

    async updateCouponInDB(coupon) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readwrite');
            const store = transaction.objectStore('coupons');
            const request = store.put(coupon);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async deleteCoupon() {
        if (!this.currentCoupon) return;

        if (!confirm('정말로 이 쿠폰을 삭제하시겠습니까? 🌽')) return;

        try {
            await this.deleteCouponFromDB(this.currentCoupon.id);
            this.showToast('쿠폰이 삭제되었습니다 🌽', 'success');
            this.hideModal('coupon');
            this.loadCoupons();  // 목록 새로고침
            this.updateStats();
        } catch (error) {
            console.error('쿠폰 삭제 오류:', error);
            this.showToast('삭제에 실패했습니다 🌽', 'error');
        }
    }

    async deleteCouponFromDB(couponId) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readwrite');
            const store = transaction.objectStore('coupons');
            const request = store.delete(couponId);

            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    shareCoupon() {
        if (!this.currentCoupon) return;

        // 공유 쿠폰을 메모리에 저장
        this.sharedCoupons.set(this.currentCoupon.id, {
            ...this.currentCoupon,
            sharedAt: Date.now()
        });

        const shareUrl = `${window.location.origin}${window.location.pathname}?share=${this.currentCoupon.id}`;
        
        // QR 코드 생성
        const canvas = document.getElementById('qr-canvas');
        QRCode.toCanvas(canvas, shareUrl, { width: 200 }, (error) => {
            if (error) {
                console.error('QR 코드 생성 오류:', error);
                this.showToast('QR 코드 생성에 실패했습니다 🌽', 'error');
            }
        });

        document.getElementById('share-url').value = shareUrl;
        document.getElementById('share-section').classList.remove('hidden');
        
        this.showToast('공유 링크가 생성되었습니다! 🌽🔗', 'success');
    }

    copyShareLink() {
        const shareUrl = document.getElementById('share-url');
        shareUrl.select();
        document.execCommand('copy');
        this.showToast('링크가 복사되었습니다! 🌽📋', 'success');
    }

    checkSharedCoupon() {
        const urlParams = new URLSearchParams(window.location.search);
        const shareId = urlParams.get('share');
        
        if (shareId) {
            this.loadSharedCoupon(shareId);
        }
    }

    loadSharedCoupon(shareId) {
        // 메모리에서 공유 쿠폰 찾기
        const sharedCoupon = this.sharedCoupons.get(shareId);
        
        if (sharedCoupon) {
            this.displaySharedCoupon(sharedCoupon);
        } else {
            // 로컬 DB에서 찾기 (같은 브라우저에서 공유한 경우)
            this.getCouponById(shareId).then(coupon => {
                if (coupon) {
                    this.displaySharedCoupon(coupon);
                } else {
                    this.showToast('공유된 쿠폰을 찾을 수 없습니다 🌽', 'error');
                }
            });
        }
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

    displaySharedCoupon(coupon) {
        this.sharedCoupon = coupon;
        
        document.getElementById('share-coupon-image').src = coupon.imgSrc;
        document.getElementById('share-coupon-title').textContent = coupon.name;
        document.getElementById('share-coupon-brand').textContent = coupon.brand;
        document.getElementById('share-coupon-amount').textContent = coupon.amount ? coupon.amount.toLocaleString() + '원' : '정보 없음';
        document.getElementById('share-coupon-expiry').textContent = coupon.expiry || '정보 없음';
        document.getElementById('share-usage-toggle').checked = coupon.used;
        
        document.getElementById('share-page').classList.remove('hidden');
        document.getElementById('app').style.display = 'none';
    }

    hideSharePage() {
        document.getElementById('share-page').classList.add('hidden');
        document.getElementById('app').style.display = 'block';
        
        // URL에서 share 파라미터 제거
        const url = new URL(window.location);
        url.searchParams.delete('share');
        window.history.replaceState({}, document.title, url);
    }

    async updateSharedCouponUsage(used) {
        if (!this.sharedCoupon) return;

        try {
            // 로컬 DB에서 쿠폰 업데이트 시도
            const localCoupon = await this.getCouponById(this.sharedCoupon.id);
            if (localCoupon) {
                localCoupon.used = used;
                localCoupon.updatedAt = Date.now();
                await this.updateCouponInDB(localCoupon);
                
                // BroadcastChannel로 다른 탭에 알림
                this.channel.postMessage({
                    type: 'coupon-updated',
                    couponId: localCoupon.id,
                    used: used
                });
            }

            // 공유 쿠폰 상태도 업데이트
            this.sharedCoupon.used = used;
            this.sharedCoupon.updatedAt = Date.now();
            this.sharedCoupons.set(this.sharedCoupon.id, this.sharedCoupon);
            
            this.showToast(used ? '사용 완료로 변경되었습니다 🌽' : '사용 취소되었습니다 🌽', 'success');
            
        } catch (error) {
            console.error('공유 쿠폰 상태 업데이트 오류:', error);
            this.showToast('상태 변경에 실패했습니다 🌽', 'error');
        }
    }

    loadSettings() {
        const apiKeyInput = document.getElementById('api-key-input');
        if (this.apiKey) {
            apiKeyInput.value = '**'.repeat(10); // 보안을 위해 마스킹
        }
        
        // 알림 설정 로드
        const notificationsEnabled = localStorage.getItem('notifications-enabled') === 'true';
        const notify7days = localStorage.getItem('notify-7days') !== 'false';
        const notify1day = localStorage.getItem('notify-1day') !== 'false';
        
        document.getElementById('notifications-enabled').checked = notificationsEnabled;
        document.getElementById('notify-7days').checked = notify7days;
        document.getElementById('notify-1day').checked = notify1day;
    }

    saveApiKey() {
        const apiKeyInput = document.getElementById('api-key-input');
        const apiKey = apiKeyInput.value.trim();
        
        if (apiKey && apiKey !== '**'.repeat(10)) {
            this.apiKey = apiKey;
            localStorage.setItem('gemini-api-key', apiKey);
            this.showToast('API 키가 저장되었습니다 🌽🤖', 'success');
        }
        
        // 알림 설정 저장
        const notificationsEnabled = document.getElementById('notifications-enabled').checked;
        const notify7days = document.getElementById('notify-7days').checked;
        const notify1day = document.getElementById('notify-1day').checked;
        
        localStorage.setItem('notifications-enabled', notificationsEnabled);
        localStorage.setItem('notify-7days', notify7days);
        localStorage.setItem('notify-1day', notify1day);
        
        if (notificationsEnabled) {
            this.requestNotificationPermission();
        }
    }

    async exportData() {
        try {
            const coupons = await this.getCouponsFromDB();
            const dataStr = JSON.stringify(coupons, null, 2);
            const dataBlob = new Blob([dataStr], { type: 'application/json' });
            
            const link = document.createElement('a');
            link.href = URL.createObjectURL(dataBlob);
            link.download = `conkeep-backup-${new Date().toISOString().split('T')[0]}.json`;
            link.click();
            
            this.showToast('데이터가 내보내졌습니다 🌽💾', 'success');
        } catch (error) {
            console.error('데이터 내보내기 오류:', error);
            this.showToast('내보내기에 실패했습니다 🌽', 'error');
        }
    }

    async clearAllData() {
        if (!confirm('정말로 모든 데이터를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다! 🌽⚠️')) {
            return;
        }

        try {
            const transaction = this.db.transaction(['coupons'], 'readwrite');
            const store = transaction.objectStore('coupons');
            await store.clear();
            
            this.loadCoupons();
            this.updateStats();
            this.hideModal('settings');
            this.showToast('모든 데이터가 삭제되었습니다 🌽', 'success');
        } catch (error) {
            console.error('데이터 삭제 오류:', error);
            this.showToast('삭제에 실패했습니다 🌽', 'error');
        }
    }

    // 알림 관련 메서드
    checkNotificationPermission() {
        if ('Notification' in window) {
            if (Notification.permission === 'granted') {
                // 이미 허용됨
            } else if (Notification.permission !== 'denied') {
                // 권한 요청 가능
            }
        }
    }

    requestNotificationPermission() {
        if ('Notification' in window && Notification.permission !== 'granted') {
            Notification.requestPermission().then(permission => {
                if (permission === 'granted') {
                    this.showToast('알림 권한이 허용되었습니다 🌽🔔', 'success');
                } else {
                    this.showToast('알림 권한이 거부되었습니다 🌽', 'warning');
                }
            });
        }
    }

    startNotificationScheduler() {
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
        const notificationsEnabled = localStorage.getItem('notifications-enabled') === 'true';
        if (!notificationsEnabled || Notification.permission !== 'granted') {
            return;
        }

        try {
            const coupons = await this.getCouponsFromDB();
            const notify7days = localStorage.getItem('notify-7days') !== 'false';
            const notify1day = localStorage.getItem('notify-1day') !== 'false';

            coupons.forEach(coupon => {
                if (coupon.used || !coupon.expiry) return;

                const daysLeft = this.getDaysUntilExpiry(coupon.expiry);
                
                if ((notify7days && daysLeft === 7) || (notify1day && daysLeft === 1)) {
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

    showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : type === 'warning' ? '⚠️' : 'ℹ️';
        toast.innerHTML = `${icon} ${message}`;
        
        container.appendChild(toast);
        
        setTimeout(() => {
            toast.remove();
        }, 3000);
    }

    generateId() {
        return Date.now().toString(36) + Math.random().toString(36).substr(2);
    }
}

// 앱 초기화
document.addEventListener('DOMContentLoaded', () => {
    window.conKeepApp = new ConKeepApp();
    
    // 초기 테마 설정
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
        document.body.setAttribute('data-theme', 'dark');
        document.querySelector('.theme-icon').textContent = '☀️';
    }
});

// 전역 함수 (HTML에서 호출용)
function switchTab(tabName) {
    if (window.conKeepApp) {
        window.conKeepApp.switchTab(tabName);
    }
}
