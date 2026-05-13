document.addEventListener('DOMContentLoaded', () => {
    const splashScreen = document.getElementById('splashScreen');
    const menuToggle = document.getElementById('menuToggle');
    const dots = document.querySelectorAll('.dot');
    
    // Splash Screen logic
    setTimeout(() => {
        splashScreen.classList.add('fade-out');
        // Remove from DOM after fade out to prevent interaction issues
        setTimeout(() => {
            splashScreen.remove();
        }, 600);
    }, 2000); // 2 seconds splash

    // Hamburger menu toggle effect
    menuToggle.addEventListener('click', () => {
        menuToggle.classList.toggle('active');
        console.log('Menu toggled');
    });

    // Simple dot interaction
    dots.forEach((dot, index) => {
        dot.addEventListener('click', () => {
            dots.forEach(d => d.classList.remove('active'));
            dot.classList.add('active');
        });
    });
});
