"use client";

import Link from "next/link";
import { ArrowRight, BadgeCheck, Check, ChevronLeft, ChevronRight, RefreshCw } from "lucide-react";
import { useEffect, useState } from "react";
import Photo, { type PhotoName } from "@/components/landing/Photo";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { t, tx } from "@/lib/messages";

const slides: { key: "permanent" | "contract" | "rpo"; photo: PhotoName; position: string }[] = [
  { key: "permanent", photo: "teamCenter", position: "50% 30%" },
  { key: "contract", photo: "teamLeft", position: "60% 40%" },
  { key: "rpo", photo: "teamRight", position: "50% 30%" },
];

export default function HeroSlider() {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [reduced, setReduced] = useState(false);

  useEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    setReduced(query.matches);
    const listener = (event: MediaQueryListEvent) => setReduced(event.matches);
    query.addEventListener("change", listener);
    return () => query.removeEventListener("change", listener);
  }, []);

  useEffect(() => {
    if (paused || reduced) return;
    const timer = window.setInterval(() => setIndex((value) => (value + 1) % slides.length), 6500);
    return () => window.clearInterval(timer);
  }, [paused, reduced]);

  const go = (next: number) => setIndex((next + slides.length) % slides.length);
  const key = slides[index].key;

  return (
    <section
      id="top"
      aria-roledescription="carousel"
      aria-label={t("landing.hero.label")}
      className="relative overflow-hidden border-b border-line"
      style={{
        background:
          "radial-gradient(60rem 28rem at 10% -10%, var(--blob-a), transparent), radial-gradient(44rem 24rem at 100% 100%, var(--blob-b), transparent), var(--bg)",
      }}
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={() => setPaused(false)}
    >
      <div className="mx-auto grid max-w-6xl items-center gap-14 px-4 py-14 lg:grid-cols-[1.05fr_0.95fr] lg:py-20">
        <div className="grid gap-6" aria-live={paused ? "polite" : "off"}>
          <Badge variant="accent" className="w-fit">
            {t("landing.hero.badge")}
          </Badge>
          <h1 className="m-0 max-w-xl text-4xl font-bold leading-[1.08] tracking-tight sm:text-5xl lg:text-6xl">
            {tx(`landing.hero.slides.${key}.title`)}
          </h1>
          <p className="muted m-0 max-w-lg text-lg">{tx(`landing.hero.slides.${key}.text`)}</p>
          <div className="flex flex-wrap gap-3">
            <Button asChild size="default">
              <Link href="/#contact">
                {t("landing.hero.hire")}
                <ArrowRight aria-hidden="true" className="size-4" />
              </Link>
            </Button>
            <Button asChild variant="outline">
              <Link href="/apply">{t("landing.hero.findJob")}</Link>
            </Button>
          </div>
          <ul className="m-0 grid list-none gap-2 p-0 text-sm sm:grid-cols-1">
            {[1, 2, 3].map((point) => (
              <li key={point} className="flex items-center gap-2">
                <span className="grid size-5 shrink-0 place-items-center rounded-full bg-accent-solid/15 text-accent">
                  <Check aria-hidden="true" className="size-3" />
                </span>
                {tx(`landing.hero.points.${point}`)}
              </li>
            ))}
          </ul>
          <div className="flex items-center gap-2 pt-1">
            <Button
              type="button"
              variant="outline"
              size="icon"
              aria-label={t("landing.hero.previous")}
              onClick={() => go(index - 1)}
            >
              <ChevronLeft aria-hidden="true" className="size-4" />
            </Button>
            <Button
              type="button"
              variant="outline"
              size="icon"
              aria-label={t("landing.hero.next")}
              onClick={() => go(index + 1)}
            >
              <ChevronRight aria-hidden="true" className="size-4" />
            </Button>
            <div className="ml-2 flex items-center gap-2">
              {slides.map((slide, position) => (
                <button
                  key={slide.key}
                  type="button"
                  aria-label={tx(`landing.hero.slides.${slide.key}.title`)}
                  aria-current={position === index}
                  onClick={() => go(position)}
                  className={`h-2 cursor-pointer rounded-full border-0 transition-all ${
                    position === index ? "w-8 bg-accent-solid" : "w-2 bg-line"
                  }`}
                />
              ))}
            </div>
          </div>
        </div>

        <div className="relative mx-auto w-full max-w-sm pb-8 pl-4 pr-4 lg:max-w-md">
          <div
            aria-hidden="true"
            className="absolute inset-y-0 right-0 left-8 translate-x-0 rounded-t-[999px] rounded-b-[2.5rem] border-2 border-accent-solid/30"
          />
          <div className="relative aspect-[4/5] overflow-hidden rounded-t-[999px] rounded-b-[2.5rem] bg-surface shadow-2xl">
            {slides.map((slide, position) => (
              <div
                key={slide.key}
                aria-hidden={position !== index}
                className={`absolute inset-0 transition-opacity duration-700 ${
                  position === index ? "opacity-100" : "opacity-0"
                }`}
              >
                <Photo
                  name={slide.photo}
                  priority={position === 0}
                  decorative={position !== index}
                  className="scale-105"
                />
              </div>
            ))}
            <div className="pointer-events-none absolute inset-x-0 bottom-0 h-1/4 bg-gradient-to-t from-black/25 to-transparent" />
          </div>
          <div className="float-y absolute -left-1 top-24 flex items-center gap-2 rounded-2xl border border-line bg-surface/85 px-3 py-2 text-sm font-medium shadow-lg backdrop-blur sm:-left-6">
            <span className="grid size-8 place-items-center rounded-full bg-accent-solid text-accent-fg">
              <BadgeCheck aria-hidden="true" className="size-4" />
            </span>
            {t("landing.about.promises.pay.title")}
          </div>
          <div
            className="float-y absolute -right-1 bottom-24 flex items-center gap-2 rounded-2xl border border-line bg-surface/85 px-3 py-2 text-sm font-medium shadow-lg backdrop-blur sm:-right-4"
            style={{ animationDelay: "1.6s" }}
          >
            <span className="grid size-8 place-items-center rounded-full bg-ok text-white">
              <RefreshCw aria-hidden="true" className="size-4" />
            </span>
            {t("landing.about.promises.replace.title")}
          </div>
        </div>
      </div>
    </section>
  );
}
