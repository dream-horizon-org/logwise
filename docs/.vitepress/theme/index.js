import DefaultTheme from 'vitepress/theme'
import { onMounted, onUpdated, watch } from 'vue'
import { useRoute } from 'vitepress'
import './custom.css'   // <-- your CSS file

export default {
    ...DefaultTheme,
    setup() {
        const route = useRoute()

        function initFAQ() {
            const faqItems = document.querySelectorAll('.faq-item');

            if (faqItems.length === 0) return;

            faqItems.forEach(item => {
                const question = item.querySelector('.faq-question');
                if (!question) return;

                // Check if already initialized to avoid duplicate listeners
                if (question.dataset.faqInitialized === 'true') {
                    return;
                }

                // Mark as initialized
                question.dataset.faqInitialized = 'true';

                const answer = item.querySelector('.faq-answer');
                const arrow = item.querySelector('.faq-arrow');

                if (!answer || !arrow) return;

                question.addEventListener('click', function () {
                    // Get fresh references in case DOM changed
                    const currentAnswer = item.querySelector('.faq-answer');
                    const currentArrow = item.querySelector('.faq-arrow');

                    if (!currentAnswer || !currentArrow) return;

                    const isOpen = currentAnswer.style.maxHeight && currentAnswer.style.maxHeight !== '0px';

                    // Close all other items
                    const allFaqItems = document.querySelectorAll('.faq-item');
                    allFaqItems.forEach(otherItem => {
                        if (otherItem !== item) {
                            const otherAnswer = otherItem.querySelector('.faq-answer');
                            const otherArrow = otherItem.querySelector('.faq-arrow');
                            if (otherAnswer && otherArrow) {
                                otherAnswer.style.maxHeight = '0px';
                                otherArrow.style.transform = 'rotate(0deg)';
                            }
                        }
                    });

                    // Toggle current item
                    if (isOpen) {
                        currentAnswer.style.maxHeight = '0px';
                        currentArrow.style.transform = 'rotate(0deg)';
                    } else {
                        currentAnswer.style.maxHeight = currentAnswer.scrollHeight + 'px';
                        currentArrow.style.transform = 'rotate(180deg)';
                    }
                });
            });
        }

        function initializeFAQ() {
            // Small delay to ensure DOM is fully rendered
            setTimeout(() => {
                initFAQ();
            }, 100);
        }

        onMounted(() => {
            initializeFAQ();
        });

        onUpdated(() => {
            initializeFAQ();
        });

        // Watch for route changes - this is the key fix for navigation
        watch(() => route.path, () => {
            // Re-initialize after route change
            // The DOM is replaced on navigation, so we don't need to reset flags
            initializeFAQ();
        }, { immediate: false });
    }
}
