"use client";

import Link from "next/link";
import { ArrowRight, ChevronLeft, ChevronRight } from "lucide-react";
import { useEffect, useState } from "react";
import { ContractScene, PermanentScene, RpoScene } from "@/components/landing/art";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { t, tx } from "@/lib/messages";

const slideKeys = ["permanent", "contract", "rpo"] as const;
const scenes = [PermanentScene, ContractScene, RpoScene];

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
    const timer = window.setInterval(() => setIndex((value) => (value + 1) % slideKeys.length), 6500);
    return () => window.clearInterval(timer);
  }, [paused, reduced]);

  const go = (next: number) => setIndex((next + slideKeys.length) % slideKeys.length);
  const key = slideKeys[index];
  const Scene = scenes[index];

  return (
    <section
      id="top"
      aria-roledescription="carousel"
      aria-label={t("landing.hero.label")}
      className="relative overflow-hidden border-b border-line"
      style={{
        background:
          "radial-gradient(60rem 28rem at 15% -10%, var(--blob-a), transparent), radial-gradient(44rem 24rem at 100% 100%, var(--blob-b), transparent), var(--bg)",
      }}
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={() => setPaused(false)}
    >
      <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-16 lg:grid-cols-[1.1fr_0.9fr] lg:py-24">
        <div className="grid gap-5" aria-live={paused ? "polite" : "off"}>
          <Badge variant="accent" className="w-fit">
            {t("landing.hero.badge")}
          </Badge>
          <h1 className="m-0 text-4xl font-bold leading-tight tracking-tight sm:text-5xl">
            {tx(`landing.hero.slides.${key}.title`)}
          </h1>
          <p className="muted m-0 max-w-xl text-lg">{tx(`landing.hero.slides.${key}.text`)}</p>
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
          <div className="flex items-center gap-2 pt-2">
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
              {slideKeys.map((slide, position) => (
                <button
                  key={slide}
                  type="button"
                  aria-label={tx(`landing.hero.slides.${slide}.title`)}
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
        <Card className="grid gap-4 overflow-hidden p-6 sm:p-8">
          <div key={key} className="mx-auto aspect-[6/5] w-full max-w-md animate-[fade-in_0.6s_ease-out]">
            <Scene />
          </div>
          <h2 className="m-0 text-xl">{tx(`landing.services.${key}.title`)}</h2>
          <p className="muted m-0">{tx(`landing.services.${key}.text`)}</p>
          <ul className="m-0 grid list-none gap-2 p-0 text-sm">
            {[1, 2, 3].map((point) => (
              <li key={point} className="flex items-start gap-2">
                <span aria-hidden="true" className="mt-1.5 size-1.5 shrink-0 rounded-full bg-accent-solid" />
                {tx(`landing.hero.points.${point}`)}
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </section>
  );
}
