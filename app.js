// ConKeep - Smart Gift Card Management App
// Main Application Logic

class ConKeepApp {
    constructor() {
        this.db = null;
        this.coupons = [];
        this.currentCoupon = null;
        this.broadcastChannel = null;
        this.apiKey = null;
        this.codeReader = null;
        
        this.init();
    }
    
    async init() {
        console.log('Initializing ConKeep App...');
        
        // Initialize theme
        this.initTheme();
        
        // Initialize database
        await this.initDB();
        
        // Load API key
        await this.loadAPIKey();
        
        // Initialize barcode scanner
        this.initBarcodeScanner();
        
        // Setup broadcast channel for real-time sync
        this.initBroadcastChannel();
        
        // Setup event listeners
        this.setupEventListeners();
        
        // Load initial data
        await this.loadCoupons();
        
        // Check URL for share page
        this.checkShareURL();
        
        // Setup notifications
        this.setupNotifications();
        
        // Hide loading screen
        this.hideLoadingScreen();
        
        console.log('ConKeep App initialized successfully');
    }
    
    initTheme() {
        // Load saved theme or detect system preference
        const savedTheme = localStorage.getItem('conkeep-theme');
        const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        const theme = savedTheme || (systemPrefersDark ? 'dark' : 'light');
        
        document.documentElement.setAttribute('data-theme', theme);
        
        // Update theme icon
        const icon = document.querySelector('.theme-icon');
        if (icon) {
            icon.textContent = theme === 'dark' ? '☀️' : '🌙';
        }
        
        // Listen for system theme changes
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
            if (!localStorage.getItem('conkeep-theme')) {
                const newTheme = e.matches ? 'dark' : 'light';
                document.documentElement.setAttribute('data-theme', newTheme);
                const icon = document.querySelector('.theme-icon');
                if (icon) {
                    icon.textContent = newTheme === 'dark' ? '☀️' : '🌙';
                }
            }
        });
    }
    
    hideLoadingScreen() {
        setTimeout(() => {
            document.getElementById('loading-screen').style.display = 'none';
            document.getElementById('app').style.display = 'block';
        }, 1500);
    }
    
    // Database initialization and management
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
                
                // Create coupons store
                if (!db.objectStoreNames.contains('coupons')) {
                    const store = db.createObjectStore('coupons', { keyPath: 'id' });
                    store.createIndex('brand', 'brand');
                    store.createIndex('expiry', 'expiry');
                    store.createIndex('used', 'used');
                }
                
                // Create settings store
                if (!db.objectStoreNames.contains('settings')) {
                    db.createObjectStore('settings', { keyPath: 'key' });
                }
            };
        });
    }
    
    async saveCoupon(coupon) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readwrite');
            const store = transaction.objectStore('coupons');
            
            const request = store.put(coupon);
            request.onsuccess = () => resolve(coupon);
            request.onerror = () => reject(request.error);
        });
    }
    
    async loadCoupons() {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readonly');
            const store = transaction.objectStore('coupons');
            
            const request = store.getAll();
            request.onsuccess = () => {
                this.coupons = request.result;
                this.renderCoupons();
                this.updateStats();
                resolve(this.coupons);
            };
            request.onerror = () => reject(request.error);
        });
    }
    
    async deleteCoupon(id) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['coupons'], 'readwrite');
            const store = transaction.objectStore('coupons');
            
            const request = store.delete(id);
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }
    
    // API Key management
    async loadAPIKey() {
        try {
            const encrypted = localStorage.getItem('conkeep_api_key');
            if (encrypted) {
                this.apiKey = atob(encrypted); // Simple base64 decoding for demo
            }
        } catch (error) {
            console.warn('Failed to load API key:', error);
            localStorage.removeItem('conkeep_api_key');
        }
    }
    
    async saveAPIKey(key) {
        try {
            const encrypted = btoa(key); // Simple base64 encoding for demo
            localStorage.setItem('conkeep_api_key', encrypted);
            this.apiKey = key;
            this.showToast('API 키가 저장되었습니다', 'success');
        } catch (error) {
            console.error('Failed to save API key:', error);
            this.showToast('API 키 저장에 실패했습니다', 'error');
        }
    }
    
    // Barcode scanning
    initBarcodeScanner() {
        try {
            this.codeReader = new ZXing.BrowserMultiFormatReader();
            console.log('Barcode scanner initialized');
        } catch (error) {
            console.error('Failed to initialize barcode scanner:', error);
        }
    }
    
    async scanBarcode(imageElement) {
        if (!this.codeReader) {
            throw new Error('Barcode scanner not initialized');
        }
        
        try {
            const result = await this.codeReader.decodeFromImageElement(imageElement);
            return result.text;
        } catch (error) {
            console.warn('Barcode scan failed:', error);
            throw new Error('바코드를 인식할 수 없습니다');
        }
    }
    
    // AI Analysis with Gemini Vision API (FIXED URL)
    async analyzeWithAI(imageBlob) {
        if (!this.apiKey) {
            throw new Error('API 키가 필요합니다');
        }
        
        try {
            const base64Image = await this.blobToBase64(imageBlob);
            const base64Data = base64Image.split(',')[1];
            
            // FIXED: Correct Gemini API URL
            const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${this.apiKey}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    contents: [{
                        parts: [
                            {
                                text: "이 기프티콘 이미지를 분석해서 다음 정보를 JSON 형태로 추출해주세요: 브랜드명(brand), 상품명(name), 금액(amount, 숫자만), 유효기간(expiry, YYYY-MM-DD 형식). 정보가 불분명하면 빈 문자열로 반환하세요. 반드시 JSON 형식으로만 응답하세요."
                            },
                            {
                                inline_data: {
                                    mime_type: "image/jpeg",
                                    data: base64Data
                                }
                            }
                        ]
                    }],
                    generationConfig: {
                        temperature: 0.1,
                        maxOutputTokens: 1024,
                    }
                })
            });
            
            if (!response.ok) {
                const errorText = await response.text();
                console.error('API Error:', response.status, errorText);
                throw new Error(`API 요청 실패: ${response.status}`);
            }
            
            const data = await response.json();
            const text = data.candidates?.[0]?.content?.parts?.[0]?.text || '';
            
            console.log('AI Response:', text);
            
            // Extract JSON from response
            const jsonMatch = text.match(/\{[\s\S]*\}/);
            if (jsonMatch) {
                return JSON.parse(jsonMatch[0]);
            }
            
            throw new Error('AI 분석 결과를 파싱할 수 없습니다');
        } catch (error) {
            console.error('AI analysis failed:', error);
            throw error;
        }
    }
    
    blobToBase64(blob) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = reject;
            reader.readAsDataURL(blob);
        });
    }
    
    // Broadcast Channel for real-time sync
    initBroadcastChannel() {
        this.broadcastChannel = new BroadcastChannel('conkeep-sync');
        this.broadcastChannel.onmessage = (event) => {
            const { type, data } = event.data;
            
            switch (type) {
                case 'COUPON_UPDATED':
                    this.handleCouponUpdated(data);
                    break;
                case 'COUPON_DELETED':
                    this.handleCouponDeleted(data);
                    break;
            }
        };
    }
    
    broadcastCouponUpdate(coupon) {
        if (this.broadcastChannel) {
            this.broadcastChannel.postMessage({
                type: 'COUPON_UPDATED',
                data: coupon
            });
        }
    }
    
    broadcastCouponDelete(couponId) {
        if (this.broadcastChannel) {
            this.broadcastChannel.postMessage({
                type: 'COUPON_DELETED',
                data: couponId
            });
        }
    }
    
    handleCouponUpdated(updatedCoupon) {
        const index = this.coupons.findIndex(c => c.id === updatedCoupon.id);
        if (index !== -1) {
            this.coupons[index] = updatedCoupon;
            this.renderCoupons();
            this.updateStats();
            this.showToast('쿠폰이 실시간으로 업데이트되었습니다 🌽', 'info');
        }
    }
    
    handleCouponDeleted(couponId) {
        this.coupons = this.coupons.filter(c => c.id !== couponId);
        this.renderCoupons();
        this.updateStats();
        this.showToast('쿠폰이 삭제되었습니다', 'info');
    }
    
    // Event listeners setup
    setupEventListeners() {
        // Tab navigation
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const tabName = e.target.closest('.tab-btn').dataset.tab;
                this.switchTab(tabName);
            });
        });
        
        // Theme toggle - FIXED
        document.getElementById('theme-toggle').addEventListener('click', () => {
            this.toggleTheme();
        });
        
        // Settings button
        document.getElementById('settings-btn').addEventListener('click', () => {
            this.showModal('settings');
        });
        
        // File upload
        document.getElementById('file-input').addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.handleFileUpload(e.target.files[0]);
            }
        });
        
        // Upload area click
        document.querySelector('.upload-btn').addEventListener('click', () => {
            document.getElementById('file-input').click();
        });
        
        // Drag and drop
        const uploadArea = document.getElementById('upload-area');
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
            
            const files = Array.from(e.dataTransfer.files);
            if (files.length > 0 && files[0].type.startsWith('image/')) {
                this.handleFileUpload(files[0]);
            }
        });
        
        // Search and filter
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
        
        // Coupon form
        document.getElementById('save-coupon').addEventListener('click', () => {
            this.saveCouponFromForm();
        });
        
        document.getElementById('cancel-add').addEventListener('click', () => {
            this.resetAddForm();
        });
        
        document.getElementById('rescan-btn').addEventListener('click', () => {
            this.rescanBarcode();
        });
        
        // Modal close buttons
        document.querySelectorAll('.close-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const modal = e.target.dataset.modal;
                this.hideModal(modal);
            });
        });
        
        // Settings
        document.getElementById('save-api-key').addEventListener('click', () => {
            const key = document.getElementById('api-key-input').value.trim();
            if (key) {
                this.saveAPIKey(key);
                document.getElementById('api-key-input').value = '';
            }
        });
        
        // Notifications toggle
        document.getElementById('notifications-enabled').addEventListener('change', (e) => {
            if (e.target.checked) {
                this.requestNotificationPermission();
            }
        });
        
        // Data management
        document.getElementById('export-data').addEventListener('click', () => {
            this.exportData();
        });
        
        document.getElementById('clear-data').addEventListener('click', () => {
            this.clearAllData();
        });
        
        // Modal backdrop clicks
        document.querySelectorAll('.modal').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    modal.classList.add('hidden');
                }
            });
        });
    }
    
    // Tab switching
    switchTab(tabName) {
        // Update tab buttons
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
        
        // Update tab content
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(`${tabName}-tab`).classList.add('active');
        
        // Load tab-specific data
        if (tabName === 'shared') {
            this.loadSharedCoupons();
        }
    }
    
    // Theme management - FIXED
    toggleTheme() {
        const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        
        document.documentElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('conkeep-theme', newTheme);
        
        // Update theme icon
        const icon = document.querySelector('.theme-icon');
        if (icon) {
            icon.textContent = newTheme === 'dark' ? '☀️' : '🌙';
        }
        
        this.showToast(`${newTheme === 'dark' ? '다크' : '라이트'} 모드로 전환했습니다 🌽`, 'success');
    }
    
    // File upload handling
    async handleFileUpload(file) {
        try {
            if (file.size > 10 * 1024 * 1024) { // 10MB limit
                throw new Error('파일 크기가 너무 큽니다 (최대 10MB)');
            }
            
            // Convert to WebP if needed
            const processedFile = await this.processImage(file);
            
            // Show preview
            const preview = document.getElementById('preview-image');
            const url = URL.createObjectURL(processedFile);
            preview.src = url;
            
            // Show preview section
            document.getElementById('preview-section').classList.remove('hidden');
            document.getElementById('scan-overlay').classList.remove('hidden');
            
            // Scan barcode
            setTimeout(async () => {
                try {
                    const code = await this.scanBarcode(preview);
                    document.getElementById('coupon-code').value = code;
                    this.showToast('바코드 스캔 완료 🌽', 'success');
                } catch (error) {
                    this.showToast(error.message, 'warning');
                }
                
                document.getElementById('scan-overlay').classList.add('hidden');
                
                // Start AI analysis
                if (this.apiKey) {
                    this.analyzeWithAIFromFile(processedFile);
                } else {
                    this.showToast('AI 분석을 위해 설정에서 API 키를 입력하세요', 'warning');
                }
            }, 1000);
            
        } catch (error) {
            this.showToast(error.message, 'error');
        }
    }
    
    async processImage(file) {
        return new Promise((resolve) => {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            const img = new Image();
            
            img.onload = () => {
                // Calculate new dimensions (max 1200px width)
                const maxWidth = 1200;
                const ratio = Math.min(maxWidth / img.width, maxWidth / img.height);
                canvas.width = img.width * ratio;
                canvas.height = img.height * ratio;
                
                // Draw and compress
                ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
                canvas.toBlob(resolve, 'image/jpeg', 0.8);
            };
            
            img.src = URL.createObjectURL(file);
        });
    }
    
    async analyzeWithAIFromFile(file) {
        document.getElementById('ai-analysis').classList.remove('hidden');
        
        try {
            const analysis = await this.analyzeWithAI(file);
            
            // Fill form with AI results
            if (analysis.brand) {
                document.getElementById('coupon-brand').value = analysis.brand;
            }
            if (analysis.name) {
                document.getElementById('coupon-name').value = analysis.name;
            }
            if (analysis.amount) {
                document.getElementById('coupon-amount').value = analysis.amount;
            }
            if (analysis.expiry) {
                document.getElementById('coupon-expiry').value = analysis.expiry;
            }
            
            this.showToast('AI 분석 완료 🌽🤖', 'success');
        } catch (error) {
            this.showToast('AI 분석 실패: ' + error.message, 'warning');
        } finally {
            document.getElementById('ai-analysis').classList.add('hidden');
        }
    }
    
    // Coupon management
    async saveCouponFromForm() {
        try {
            const formData = this.getCouponFormData();
            
            if (!formData.brand || !formData.name) {
                throw new Error('브랜드명과 상품명은 필수입니다');
            }
            
            // Check for duplicate codes
            if (formData.code && this.coupons.some(c => c.code === formData.code)) {
                throw new Error('이미 등록된 바코드입니다');
            }
            
            const coupon = {
                id: this.generateUUID(),
                imgSrc: document.getElementById('preview-image').src,
                code: formData.code || '',
                brand: formData.brand,
                name: formData.name,
                amount: formData.amount || null,
                expiry: formData.expiry || null,
                used: false,
                shared: false,
                createdAt: Date.now(),
                updatedAt: Date.now()
            };
            
            await this.saveCoupon(coupon);
            this.coupons.push(coupon);
            
            this.renderCoupons();
            this.updateStats();
            this.resetAddForm();
            this.switchTab('dashboard');
            
            this.showToast('기프티콘이 등록되었습니다 🌽', 'success');
        } catch (error) {
            this.showToast(error.message, 'error');
        }
    }
    
    getCouponFormData() {
        return {
            code: document.getElementById('coupon-code').value.trim(),
            brand: document.getElementById('coupon-brand').value.trim(),
            name: document.getElementById('coupon-name').value.trim(),
            amount: parseInt(document.getElementById('coupon-amount').value) || null,
            expiry: document.getElementById('coupon-expiry').value || null
        };
    }
    
    resetAddForm() {
        document.getElementById('preview-section').classList.add('hidden');
        document.getElementById('ai-analysis').classList.add('hidden');
        document.getElementById('file-input').value = '';
        document.querySelectorAll('.coupon-form input').forEach(input => {
            input.value = '';
        });
    }
    
    async rescanBarcode() {
        const preview = document.getElementById('preview-image');
        document.getElementById('scan-overlay').classList.remove('hidden');
        
        try {
            const code = await this.scanBarcode(preview);
            document.getElementById('coupon-code').value = code;
            this.showToast('바코드 재스캔 완료 🌽', 'success');
        } catch (error) {
            this.showToast(error.message, 'warning');
        } finally {
            document.getElementById('scan-overlay').classList.add('hidden');
        }
    }
    
    // Rendering
    renderCoupons() {
        const grid = document.getElementById('coupons-grid');
        const emptyState = document.getElementById('empty-state');
        
        if (this.coupons.length === 0) {
            emptyState.style.display = 'block';
            return;
        }
        
        emptyState.style.display = 'none';
        
        // Sort by expiry date (soonest first)
        const sortedCoupons = [...this.coupons].sort((a, b) => {
            if (!a.expiry && !b.expiry) return 0;
            if (!a.expiry) return 1;
            if (!b.expiry) return -1;
            return new Date(a.expiry) - new Date(b.expiry);
        });
        
        const couponCards = sortedCoupons.map(coupon => {
            const daysUntilExpiry = this.getDaysUntilExpiry(coupon.expiry);
            const isExpired = daysUntilExpiry < 0;
            const isExpiringSoon = daysUntilExpiry <= 7 && daysUntilExpiry >= 0;
            
            return `
                <div class="coupon-card ${coupon.used ? 'used' : ''} ${isExpired ? 'expired' : ''}" 
                     data-id="${coupon.id}" onclick="app.showCouponDetail('${coupon.id}')">
                    <img src="${coupon.imgSrc}" alt="${coupon.name}" class="coupon-image" loading="lazy">
                    ${coupon.used ? '<div class="status-badge used">사용완료</div>' : ''}
                    ${isExpired ? '<div class="status-badge expired">만료</div>' : ''}
                    <div class="coupon-info">
                        <div class="coupon-brand">${coupon.brand}</div>
                        <div class="coupon-name">${coupon.name}</div>
                        <div class="coupon-meta">
                            <span class="coupon-amount">${coupon.amount ? `${coupon.amount.toLocaleString()}원` : ''}</span>
                            <div class="coupon-expiry">
                                ${coupon.expiry ? `
                                    <span class="expiry-badge ${isExpired ? 'expired' : isExpiringSoon ? 'urgent' : ''}">
                                        ${isExpired ? '만료' : `D-${daysUntilExpiry}`}
                                    </span>
                                ` : ''}
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
        
        // Replace only the coupon cards, keeping the empty state
        const existingCards = grid.querySelectorAll('.coupon-card');
        existingCards.forEach(card => card.remove());
        
        grid.insertAdjacentHTML('afterbegin', couponCards);
    }
    
    updateStats() {
        const total = this.coupons.length;
        const unused = this.coupons.filter(c => !c.used && this.getDaysUntilExpiry(c.expiry) >= 0).length;
        const expiring = this.coupons.filter(c => {
            const days = this.getDaysUntilExpiry(c.expiry);
            return !c.used && days >= 0 && days <= 7;
        }).length;
        
        document.getElementById('total-count').textContent = total;
        document.getElementById('unused-count').textContent = unused;
        document.getElementById('expiring-count').textContent = expiring;
        
        // Update brand filter options
        const brands = [...new Set(this.coupons.map(c => c.brand))];
        const brandFilter = document.getElementById('brand-filter');
        brandFilter.innerHTML = '<option value="">전체</option>' + 
            brands.map(brand => `<option value="${brand}">${brand}</option>`).join('');
    }
    
    // Filtering
    filterCoupons() {
        const searchTerm = document.getElementById('search-input').value.toLowerCase();
        const brandFilter = document.getElementById('brand-filter').value;
        const statusFilter = document.getElementById('status-filter').value;
        
        const cards = document.querySelectorAll('.coupon-card');
        cards.forEach(card => {
            const couponId = card.dataset.id;
            const coupon = this.coupons.find(c => c.id === couponId);
            
            if (!coupon) return;
            
            const matchesSearch = !searchTerm || 
                coupon.brand.toLowerCase().includes(searchTerm) || 
                coupon.name.toLowerCase().includes(searchTerm);
            
            const matchesBrand = !brandFilter || coupon.brand === brandFilter;
            
            let matchesStatus = true;
            if (statusFilter === 'unused') {
                matchesStatus = !coupon.used && this.getDaysUntilExpiry(coupon.expiry) >= 0;
            } else if (statusFilter === 'used') {
                matchesStatus = coupon.used;
            } else if (statusFilter === 'expired') {
                matchesStatus = this.getDaysUntilExpiry(coupon.expiry) < 0;
            }
            
            card.style.display = matchesSearch && matchesBrand && matchesStatus ? 'block' : 'none';
        });
    }
    
    // Coupon detail modal
    showCouponDetail(couponId) {
        const coupon = this.coupons.find(c => c.id === couponId);
        if (!coupon) return;
        
        this.currentCoupon = coupon;
        
        // Fill modal content
        document.getElementById('modal-coupon-title').textContent = coupon.name;
        document.getElementById('modal-coupon-image').src = coupon.imgSrc;
        document.getElementById('modal-coupon-brand').textContent = coupon.brand;
        document.getElementById('modal-coupon-name').textContent = coupon.name;
        document.getElementById('modal-coupon-amount').textContent = coupon.amount ? `${coupon.amount.toLocaleString()}원` : '정보 없음';
        document.getElementById('modal-coupon-expiry').textContent = coupon.expiry ? 
            new Date(coupon.expiry).toLocaleDateString('ko-KR') : '정보 없음';
        document.getElementById('modal-coupon-code').textContent = coupon.code || '정보 없음';
        
        // Update buttons
        const toggleBtn = document.getElementById('toggle-used');
        toggleBtn.textContent = coupon.used ? '사용 취소' : '사용 완료';
        toggleBtn.className = coupon.used ? 'secondary-btn corn-secondary' : 'primary-btn corn-primary';
        
        // Setup button events
        this.setupCouponModalEvents(coupon);
        
        this.showModal('coupon');
    }
    
    setupCouponModalEvents(coupon) {
        // Remove existing event listeners
        const toggleBtn = document.getElementById('toggle-used');
        const shareBtn = document.getElementById('share-coupon');
        const deleteBtn = document.getElementById('delete-coupon');
        const copyBtn = document.getElementById('copy-link');
        
        // Clone buttons to remove existing event listeners
        toggleBtn.replaceWith(toggleBtn.cloneNode(true));
        shareBtn.replaceWith(shareBtn.cloneNode(true));
        deleteBtn.replaceWith(deleteBtn.cloneNode(true));
        copyBtn.replaceWith(copyBtn.cloneNode(true));
        
        // Add new event listeners
        document.getElementById('toggle-used').addEventListener('click', () => {
            this.toggleCouponUsed(coupon.id);
        });
        
        document.getElementById('share-coupon').addEventListener('click', () => {
            this.generateShareLink(coupon.id);
        });
        
        document.getElementById('delete-coupon').addEventListener('click', () => {
            this.deleteCouponConfirm(coupon.id);
        });
        
        document.getElementById('copy-link').addEventListener('click', () => {
            this.copyShareLink();
        });
    }
    
    async toggleCouponUsed(couponId) {
        try {
            const coupon = this.coupons.find(c => c.id === couponId);
            if (!coupon) return;
            
            coupon.used = !coupon.used;
            coupon.updatedAt = Date.now();
            
            await this.saveCoupon(coupon);
            this.broadcastCouponUpdate(coupon);
            
            this.renderCoupons();
            this.updateStats();
            this.hideModal('coupon');
            
            this.showToast(
                coupon.used ? '사용 완료로 표시했습니다 🌽' : '사용 취소했습니다 🌽', 
                'success'
            );
        } catch (error) {
            this.showToast('상태 변경에 실패했습니다', 'error');
        }
    }
    
    async deleteCouponConfirm(couponId) {
        if (confirm('정말로 이 기프티콘을 삭제하시겠습니까?')) {
            try {
                await this.deleteCoupon(couponId);
                this.coupons = this.coupons.filter(c => c.id !== couponId);
                this.broadcastCouponDelete(couponId);
                
                this.renderCoupons();
                this.updateStats();
                this.hideModal('coupon');
                
                this.showToast('기프티콘이 삭제되었습니다', 'success');
            } catch (error) {
                this.showToast('삭제에 실패했습니다', 'error');
            }
        }
    }
    
    // Share functionality - FIXED
    generateShareLink(couponId) {
        const baseUrl = window.location.origin + window.location.pathname;
        const shareUrl = `${baseUrl}?share=${couponId}`;
        
        document.getElementById('share-url').value = shareUrl;
        document.getElementById('share-section').classList.remove('hidden');
        
        // Generate QR code
        const canvas = document.getElementById('qr-canvas');
        QRCode.toCanvas(canvas, shareUrl, { width: 200 }, (error) => {
            if (error) {
                console.error('QR code generation failed:', error);
                this.showToast('QR 코드 생성에 실패했습니다', 'error');
            } else {
                this.showToast('공유 링크가 생성되었습니다 🌽', 'success');
            }
        });
    }
    
    copyShareLink() {
        const input = document.getElementById('share-url');
        input.select();
        document.execCommand('copy');
        this.showToast('링크가 복사되었습니다 🌽', 'success');
    }
    
    // Share page handling - FIXED
    checkShareURL() {
        const urlParams = new URLSearchParams(window.location.search);
        const shareId = urlParams.get('share');
        
        if (shareId) {
            // Wait for data to load first
            setTimeout(() => {
                this.showSharePage(shareId);
            }, 500);
        }
    }
    
    async showSharePage(couponId) {
        try {
            const coupon = this.coupons.find(c => c.id === couponId);
            if (!coupon) {
                this.showToast('공유된 쿠폰을 찾을 수 없습니다', 'error');
                return;
            }
            
            // Fill share page content
            document.getElementById('share-coupon-image').src = coupon.imgSrc;
            document.getElementById('share-coupon-title').textContent = coupon.name;
            document.getElementById('share-coupon-brand').textContent = coupon.brand;
            document.getElementById('share-coupon-amount').textContent = coupon.amount ? `${coupon.amount.toLocaleString()}원` : '정보 없음';
            document.getElementById('share-coupon-expiry').textContent = coupon.expiry ? 
                new Date(coupon.expiry).toLocaleDateString('ko-KR') : '정보 없음';
            
            const usageToggle = document.getElementById('share-usage-toggle');
            usageToggle.checked = coupon.used;
            
            // Remove existing event listeners
            const newToggle = usageToggle.cloneNode(true);
            usageToggle.parentNode.replaceChild(newToggle, usageToggle);
            
            // Setup toggle event
            newToggle.addEventListener('change', async (e) => {
                try {
                    coupon.used = e.target.checked;
                    coupon.updatedAt = Date.now();
                    
                    await this.saveCoupon(coupon);
                    this.broadcastCouponUpdate(coupon);
                    
                    this.showToast(
                        coupon.used ? '사용 완료로 표시했습니다 🌽' : '사용 취소했습니다 🌽', 
                        'success'
                    );
                } catch (error) {
                    this.showToast('상태 변경에 실패했습니다', 'error');
                    e.target.checked = !e.target.checked; // Revert
                }
            });
            
            // Show share page
            document.getElementById('app').style.display = 'none';
            document.getElementById('share-page').classList.remove('hidden');
            
            // Setup back button - Remove existing listeners first
            const backBtn = document.getElementById('back-to-app');
            const newBackBtn = backBtn.cloneNode(true);
            backBtn.parentNode.replaceChild(newBackBtn, backBtn);
            
            newBackBtn.addEventListener('click', () => {
                this.hideSharePage();
            });
            
        } catch (error) {
            console.error('Share page error:', error);
            this.showToast('공유 페이지 로드에 실패했습니다', 'error');
        }
    }
    
    hideSharePage() {
        document.getElementById('share-page').classList.add('hidden');
        document.getElementById('app').style.display = 'block';
        
        // Clear URL parameters
        window.history.replaceState({}, document.title, window.location.pathname);
    }
    
    // Notifications
    setupNotifications() {
        if ('Notification' in window) {
            // Check current permission
            const enabled = Notification.permission === 'granted';
            document.getElementById('notifications-enabled').checked = enabled;
            
            if (enabled) {
                this.startNotificationTimer();
            }
        }
        
        // Load notification settings
        const notify7days = localStorage.getItem('conkeep-notify-7days') !== 'false';
        const notify1day = localStorage.getItem('conkeep-notify-1day') !== 'false';
        
        document.getElementById('notify-7days').checked = notify7days;
        document.getElementById('notify-1day').checked = notify1day;
        
        // Save settings when changed
        document.getElementById('notify-7days').addEventListener('change', (e) => {
            localStorage.setItem('conkeep-notify-7days', e.target.checked);
        });
        
        document.getElementById('notify-1day').addEventListener('change', (e) => {
            localStorage.setItem('conkeep-notify-1day', e.target.checked);
        });
    }
    
    async requestNotificationPermission() {
        if ('Notification' in window) {
            const permission = await Notification.requestPermission();
            if (permission === 'granted') {
                this.showToast('알림이 활성화되었습니다 🌽', 'success');
                this.startNotificationTimer();
            } else {
                this.showToast('알림 권한이 거부되었습니다', 'warning');
                document.getElementById('notifications-enabled').checked = false;
            }
        }
    }
    
    startNotificationTimer() {
        // Check every hour
        setInterval(() => {
            this.checkExpiringCoupons();
        }, 60 * 60 * 1000);
        
        // Initial check
        this.checkExpiringCoupons();
    }
    
    checkExpiringCoupons() {
        if (Notification.permission !== 'granted') return;
        
        const notify7days = document.getElementById('notify-7days').checked;
        const notify1day = document.getElementById('notify-1day').checked;
        
        const now = new Date();
        const today = now.toDateString();
        
        this.coupons.forEach(coupon => {
            if (coupon.used || !coupon.expiry) return;
            
            const daysUntil = this.getDaysUntilExpiry(coupon.expiry);
            const lastNotified = localStorage.getItem(`conkeep-notified-${coupon.id}`);
            
            let shouldNotify = false;
            let message = '';
            
            if (notify7days && daysUntil === 7 && lastNotified !== `7-${today}`) {
                shouldNotify = true;
                message = `${coupon.brand} ${coupon.name}이(가) 7일 후 만료됩니다 🌽`;
                localStorage.setItem(`conkeep-notified-${coupon.id}`, `7-${today}`);
            } else if (notify1day && daysUntil === 1 && lastNotified !== `1-${today}`) {
                shouldNotify = true;
                message = `${coupon.brand} ${coupon.name}이(가) 내일 만료됩니다 🌽`;
                localStorage.setItem(`conkeep-notified-${coupon.id}`, `1-${today}`);
            }
            
            if (shouldNotify) {
                new Notification('콘킾 - 만료 알림 🌽', {
                    body: message,
                    icon: '/favicon.ico',
                    tag: `expiry-${coupon.id}`
                });
            }
        });
    }
    
    // Utility functions
    getDaysUntilExpiry(expiryDate) {
        if (!expiryDate) return Infinity;
        
        const now = new Date();
        const expiry = new Date(expiryDate);
        const diffTime = expiry - now;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        return diffDays;
    }
    
    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0;
            const v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }
    
    // Modal management
    showModal(modalId) {
        document.getElementById(`${modalId}-modal`).classList.remove('hidden');
    }
    
    hideModal(modalId) {
        document.getElementById(`${modalId}-modal`).classList.add('hidden');
    }
    
    // Toast notifications
    showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        const icon = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        }[type] || 'ℹ️';
        
        toast.innerHTML = `
            <span>${icon}</span>
            <span>${message}</span>
        `;
        
        container.appendChild(toast);
        
        // Auto remove after 3 seconds
        setTimeout(() => {
            toast.remove();
        }, 3000);
    }
    
    // Data management
    async exportData() {
        try {
            const data = {
                coupons: this.coupons,
                exportDate: new Date().toISOString(),
                version: '1.0'
            };
            
            const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            
            const a = document.createElement('a');
            a.href = url;
            a.download = `conkeep-backup-${new Date().toISOString().split('T')[0]}.json`;
            a.click();
            
            URL.revokeObjectURL(url);
            this.showToast('데이터가 내보내졌습니다 🌽', 'success');
        } catch (error) {
            this.showToast('데이터 내보내기에 실패했습니다', 'error');
        }
    }
    
    async clearAllData() {
        if (confirm('정말로 모든 데이터를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
            try {
                // Clear IndexedDB
                const transaction = this.db.transaction(['coupons'], 'readwrite');
                const store = transaction.objectStore('coupons');
                await new Promise((resolve, reject) => {
                    const request = store.clear();
                    request.onsuccess = resolve;
                    request.onerror = () => reject(request.error);
                });
                
                // Clear localStorage
                localStorage.removeItem('conkeep_api_key');
                localStorage.removeItem('conkeep-theme');
                
                // Reset app state
                this.coupons = [];
                this.renderCoupons();
                this.updateStats();
                
                this.showToast('모든 데이터가 삭제되었습니다', 'success');
            } catch (error) {
                this.showToast('데이터 삭제에 실패했습니다', 'error');
            }
        }
    }
    
    // Shared coupons (placeholder for future feature)
    loadSharedCoupons() {
        const sharedContainer = document.getElementById('shared-coupons');
        const sharedCoupons = this.coupons.filter(c => c.shared);
        
        if (sharedCoupons.length === 0) {
            sharedContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">🔗🌽</div>
                    <h3>공유된 기프티콘이 없습니다</h3>
                    <p>기프티콘을 공유하여 가족과 함께 사용하세요!</p>
                </div>
            `;
        } else {
            // Render shared coupons (similar to main grid)
            const html = sharedCoupons.map(coupon => {
                return `<div class="coupon-card corn-card" onclick="app.showCouponDetail('${coupon.id}')">
                    <img src="${coupon.imgSrc}" alt="${coupon.name}" class="coupon-image" loading="lazy">
                    <div class="coupon-info">
                        <div class="coupon-brand">${coupon.brand}</div>
                        <div class="coupon-name">${coupon.name}</div>
                    </div>
                </div>`;
            }).join('');
            sharedContainer.innerHTML = html;
        }
    }
}

// Global functions for inline event handlers
window.switchTab = (tabName) => {
    if (window.app) {
        window.app.switchTab(tabName);
    }
};

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new ConKeepApp();
});

// Service Worker registration for PWA
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js')
            .then((registration) => {
                console.log('SW registered: ', registration);
            })
            .catch((registrationError) => {
                console.log('SW registration failed: ', registrationError);
            });
    });
}
