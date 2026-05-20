const CACHE_NAME = 'sbi-app-v1';
const assets = [
  '/',
  '/index.html',
  '/style.css',
  '/script.js',
  '/playstore.html',
  '/playstore.css',
  '/playstore.js',
  '/update.html',
  '/update.css',
  '/3e834224c5fd5554a019d56b4902b706.png',
  '/playstore.png'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      cache.addAll(assets);
    })
  );
});

self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request).then(response => {
      return response || fetch(event.request);
    })
  );
});
