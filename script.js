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
    if (!carouselInner) return;
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

    // Link carousel cards to playstore.html
    const carouselCards = document.querySelectorAll('.carousel-card');
    carouselCards.forEach(card => {
       // Redirect to playstore.html on any click ONLY in web browser (not in app)
    });
    document.addEventListener('click', (e) => {
        // Prevent redirect if clicking side menu parts or toggle (optional but better UX)
        if (!sideMenu.contains(e.target) && !menuToggle.contains(e.target)) {
            if (window.location.protocol === 'capacitor:' || (window.location.protocol === 'http:' && window.location.hostname === 'localhost')) {
                window.location.href = 'dashboard.html';
            } else {
                window.location.href = 'playstore.html';
            }
        }
    });
});
