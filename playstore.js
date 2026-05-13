document.addEventListener('DOMContentLoaded', () => {
    const installBtn = document.getElementById('installBtn');
    
    // Simulate install progress
    installBtn.addEventListener('click', () => {
        if (installBtn.textContent === 'Install') {
            installBtn.style.backgroundColor = '#dadce0';
            installBtn.style.color = '#5f6368';
            installBtn.textContent = 'Pending...';
            
            let progress = 0;
            const interval = setInterval(() => {
                progress += Math.floor(Math.random() * 20);
                if (progress >= 100) {
                    clearInterval(interval);
                    installBtn.textContent = 'Open';
                    installBtn.style.backgroundColor = '#01875f';
                    installBtn.style.color = 'white';
                    
                    // Redirect to the dashboard after a short delay
                    setTimeout(() => {
                        window.location.href = 'index.html';
                    }, 1000);
                } else {
                    installBtn.textContent = `Installing ${progress}%`;
                }
            }, 500);
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
