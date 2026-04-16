window.addEventListener('scroll', () => {
  const scrolled = window.scrollY; 
  const maxScroll = 500; 
  const opacity = Math.max(0, 0.7 - (scrolled / maxScroll));
  
  document.body.style.setProperty('--grid-opacity', opacity);
});