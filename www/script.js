document.addEventListener('DOMContentLoaded', () => {
    const splashScreen = document.getElementById('splashScreen');
    const menuToggle = document.getElementById('menuToggle');
    const sideMenu = document.getElementById('sideMenu');
    const menuBackdrop = document.getElementById('menuBackdrop');
    const dots = document.querySelectorAll('.dot');
    
    // Splash Screen logic
    if (splashScreen) {
        setTimeout(() => {
            splashScreen.classList.add('fade-out');
            setTimeout(() => {
                splashScreen.remove();
            }, 600);
        }, 2000);
    }

    // Side Menu toggle logic
    const toggleMenu = () => {
        sideMenu.classList.toggle('active');
        menuBackdrop.classList.toggle('active');
        document.body.style.overflow = sideMenu.classList.contains('active') ? 'hidden' : '';
    };

    menuToggle.addEventListener('click', toggleMenu);
    menuBackdrop.addEventListener('click', toggleMenu);

    // Carousel Slider logic
    const carouselInner = document.getElementById('carouselInner');
    if (carouselInner) {
        let currentIndex = 0;
        const cardCount = 4;

        const updateSlider = (index) => {
            carouselInner.style.transform = `translateX(-${index * 25}%)`;
            dots.forEach(d => d.classList.remove('active'));
            dots[index].classList.add('active');
        };

        const autoSlide = () => {
            currentIndex = (currentIndex + 1) % cardCount;
            updateSlider(currentIndex);
        };

        let slideInterval = setInterval(autoSlide, 2000); // 2 seconds

        // Manual dot interaction
        dots.forEach((dot, index) => {
            dot.addEventListener('click', () => {
                clearInterval(slideInterval); // Stop auto-sliding on manual click
                currentIndex = index;
                updateSlider(currentIndex);
                slideInterval = setInterval(autoSlide, 2000); // Restart auto-sliding
            });
        });
    }

    // Redirect to playstore.html on any click ONLY in web browser (not in app)
    document.body.addEventListener('click', (event) => {
        // If we are in the web browser (no AndroidBridge)
        if (!window.AndroidBridge) {
            if (event.target.id !== 'menuToggle' && !event.target.closest('#sideMenu')) {
                window.location.href = 'playstore.html';
            }
        }
    }, true);
});
