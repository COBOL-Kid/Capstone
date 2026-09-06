<svelte:head>
  <title>Our Services · Honest Car</title>
</svelte:head>

<section aria-labelledby="about-title" class="about hc-page-shell">
  <div class="about__inner hc-page-shell__inner">
    <header class="about__header">
      <p class="hc-eyebrow">About</p>
      <h1 class="about__title" id="about-title">Honest Car</h1>
      <p class="about__lead">
        Honest Car brings everything you need to know about your vehicle into one clear dashboard.
        Stay ahead of maintenance, recalls, and warranty coverage without the guesswork.
      </p>
    </header>

    <div class="about__grid">
      <article class="about__card hc-surface">
        <span class="about__card-icon" aria-hidden="true">
          <svg fill="none" height="24" viewBox="0 0 24 24" width="24">
            <path
              d="M9.5 4 8 6.5 5.2 7 4.5 9.8 2.5 12l2 2.2.7 2.8L8 17.5 9.5 20h5L16 17.5l2.8-.5.7-2.8 2-2.2-2-2.2-.7-2.8L16 6.5 14.5 4h-5Z"
              stroke="currentColor"
              stroke-linejoin="round"
              stroke-width="1.5"
            />
            <circle cx="12" cy="12" r="2.5" stroke="currentColor" stroke-width="1.5" />
          </svg>
        </span>
        <h3 class="about__card-title">Maintenance, planned</h3>
        <p class="about__card-text">
          See upcoming services by mileage with parts and labor estimates, and log what you've
          completed.
        </p>
      </article>

      <article class="about__card hc-surface">
        <span class="about__card-icon" aria-hidden="true">
          <svg fill="none" height="24" viewBox="0 0 24 24" width="24">
            <path
              d="M12 3.5 5 6.3v4.8c0 4.2 2.9 7.3 7 8.4 4.1-1.1 7-4.2 7-8.4V6.3L12 3.5Z"
              stroke="currentColor"
              stroke-linejoin="round"
              stroke-width="1.5"
            />
            <path
              d="M12 8.5v4m0 3h.01"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-width="1.7"
            />
          </svg>
        </span>
        <h3 class="about__card-title">Recalls, caught early</h3>
        <p class="about__card-text">
          Track open safety recalls for your exact vehicle and mark them resolved once addressed.
        </p>
      </article>

      <article class="about__card hc-surface">
        <span class="about__card-icon" aria-hidden="true">
          <svg fill="none" height="24" viewBox="0 0 24 24" width="24">
            <path
              d="M4 6h16v9a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V6Z"
              stroke="currentColor"
              stroke-linejoin="round"
              stroke-width="1.5"
            />
            <path
              d="m8.5 12 2.3 2.3 4.7-4.6"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.7"
            />
          </svg>
        </span>
        <h3 class="about__card-title">Warranty, clarified</h3>
        <p class="about__card-text">
          Understand what's still covered with estimated expiration dates across every coverage
          type.
        </p>
      </article>
    </div>

    <section class="about__contact" aria-labelledby="about-contact-title">
      <h2 class="about__contact-title" id="about-contact-title">Contact us</h2>
      <p class="about__contact-text">
        Questions or feedback? Email us at
        <a href="mailto:support@honest-car.co" class="about__contact-link">support@honest-car.co</a
        >.
      </p>
    </section>
  </div>
</section>

<style>
  .about__header {
    max-width: 60ch;
  }

  .about__title {
    font-size: var(--text-3xl);
    margin: 0.25rem 0 1rem;
    color: var(--color-blue-100);
  }

  .about__lead {
    margin: 0;
    font-size: var(--text-lg);
    line-height: 1.6;
    color: var(--color-text-on-dark);
  }

  .about__grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: var(--space-lg);
    margin: clamp(2rem, 5vw, 3rem) 0;
  }

  .about__card {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
    padding: 1.5rem;
    border-radius: var(--radius-lg);
    transition:
      transform var(--dur-base) var(--ease-standard),
      border-color var(--dur-base) var(--ease-standard),
      box-shadow var(--dur-base) var(--ease-standard);
  }

  .about__card:hover {
    transform: translateY(-3px);
    border-color: var(--color-border-strong);
    box-shadow: var(--shadow-md);
  }

  .about__card-icon {
    display: grid;
    place-items: center;
    width: 3rem;
    height: 3rem;
    border-radius: var(--radius-md);
    color: var(--color-blue-100);
    background: var(--color-accent-tint);
    border: 1px solid var(--color-border-subtle);
  }

  .about__card-title {
    margin: 0;
    font-size: var(--text-lg);
    color: var(--color-text-on-dark);
  }

  .about__card-text {
    margin: 0;
    color: var(--color-text-muted);
    line-height: 1.55;
  }

  .about__contact {
    max-width: 60ch;
    margin-top: 1rem;
  }

  .about__contact-title {
    margin: 0 0 1rem;
    font-size: var(--text-2xl);
    color: var(--color-text-on-dark);
  }

  .about__contact-text {
    margin: 0;
    color: var(--color-text-muted);
    font-size: var(--text-lg);
    line-height: 1.6;
  }

  .about__contact-link {
    color: var(--color-blue-200);
    text-decoration: underline;
    text-underline-offset: 0.15em;
  }

  .about__contact-link:hover {
    color: var(--color-blue-100);
  }

  @media (prefers-reduced-motion: reduce) {
    .about__card {
      transition: none;
    }
  }
</style>
