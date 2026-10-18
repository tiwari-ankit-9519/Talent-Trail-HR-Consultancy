import { BadgeCheck, RefreshCw, ShieldCheck, type LucideIcon } from "lucide-react";
import { industries, services, type ServiceKey } from "@/components/landing/content";
import Photo, { type PhotoName } from "@/components/landing/Photo";
import Reveal from "@/components/landing/Reveal";
import { Card } from "@/components/shadcn/card";
import { t, tx } from "@/lib/messages";

function Heading({ title, text }: { title: string; text?: string }) {
  return (
    <div className="mb-10 grid max-w-2xl gap-3">
      <h2 className="m-0 text-3xl font-bold tracking-tight sm:text-4xl">{title}</h2>
      {text && <p className="muted m-0 text-lg">{text}</p>}
    </div>
  );
}

function ServiceIcon({ icon: Icon }: { icon: LucideIcon }) {
  return (
    <span className="grid size-11 place-items-center rounded-xl bg-accent-solid text-accent-fg">
      <Icon aria-hidden="true" className="size-5" />
    </span>
  );
}

function PlainService({ id, icon }: { id: ServiceKey; icon: LucideIcon }) {
  return (
    <Card className="grid h-full content-start gap-4 p-6 transition-transform duration-200 hover:-translate-y-1">
      <ServiceIcon icon={icon} />
      <div className="grid gap-2">
        <h3 className="m-0 text-lg">{tx(`landing.services.${id}.title`)}</h3>
        <p className="muted m-0 text-sm">{tx(`landing.services.${id}.text`)}</p>
      </div>
    </Card>
  );
}

export function ServicesSection() {
  return (
    <section id="services" className="scroll-mt-20 py-20">
      <div className="mx-auto max-w-6xl px-4">
        <Heading title={t("landing.services.heading")} text={t("landing.services.lead")} />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {services.map(({ key, icon }, position) => (
            <Reveal key={key} delay={position * 90} className="h-full">
              <PlainService id={key} icon={icon} />
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

const banners: { name: PhotoName; shape: string; width: string }[] = [
  { name: "bannerTalent", shape: "rounded-[2.5rem] rounded-tr-[8rem]", width: "" },
  { name: "bannerTeams", shape: "rounded-[2.5rem] rounded-tl-[8rem]", width: "md:ml-auto md:w-[92%]" },
  { name: "bannerResources", shape: "rounded-[999px]", width: "md:w-[92%]" },
];

export function ShowcaseSection() {
  return (
    <section id="showcase" className="scroll-mt-20 border-b border-line py-20">
      <div className="mx-auto max-w-6xl px-4">
        <Heading title={t("landing.showcase.heading")} text={t("landing.showcase.lead")} />
        <div className="grid grid-cols-1 gap-6">
          {banners.map(({ name, shape, width }, position) => (
            <Reveal key={name} delay={position * 60} className={`min-w-0 ${width}`}>
              <div className={`group overflow-hidden shadow-xl ${shape}`}>
                <div className="snap-x overflow-x-auto">
                  <div className="min-w-[640px] overflow-hidden md:min-w-0">
                    <Photo name={name} className="transition-transform duration-700 group-hover:scale-[1.03]" />
                  </div>
                </div>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

const promises: { key: "pay" | "replace" | "consent"; icon: LucideIcon }[] = [
  { key: "pay", icon: BadgeCheck },
  { key: "replace", icon: RefreshCw },
  { key: "consent", icon: ShieldCheck },
];

export function AboutSection() {
  return (
    <section id="about" className="scroll-mt-20 border-y border-line bg-surface py-20">
      <div className="mx-auto grid max-w-6xl items-center gap-14 px-4 lg:grid-cols-2">
        <Reveal>
          <div className="relative pb-10 pr-8">
            <div className="aspect-[4/3] overflow-hidden rounded-[2rem] shadow-2xl">
              <Photo name="teamWide" />
            </div>
            <div className="absolute bottom-0 right-0 size-32 overflow-hidden rounded-full border-4 border-surface shadow-xl sm:size-40">
              <Photo name="man" decorative className="object-[50%_15%] scale-110" />
            </div>
            <div className="absolute -left-3 bottom-4 hidden items-center gap-2 rounded-2xl border border-line bg-surface/90 px-3 py-2 text-sm font-medium shadow-lg backdrop-blur sm:flex">
              <span className="grid size-8 place-items-center rounded-full bg-ok text-white">
                <ShieldCheck aria-hidden="true" className="size-4" />
              </span>
              {t("landing.about.promises.consent.title")}
            </div>
          </div>
        </Reveal>
        <div className="grid gap-5">
          <h2 className="m-0 text-3xl font-bold tracking-tight sm:text-4xl">{t("landing.about.heading")}</h2>
          <p className="muted m-0">{t("landing.about.p1")}</p>
          <p className="muted m-0">{t("landing.about.p2")}</p>
          <ul className="m-0 mt-2 grid list-none gap-5 p-0">
            {promises.map(({ key, icon: Icon }) => (
              <li key={key} className="flex items-start gap-4">
                <span className="grid size-11 shrink-0 place-items-center rounded-full bg-accent-solid/10 text-accent">
                  <Icon aria-hidden="true" className="size-5" />
                </span>
                <span className="grid gap-1">
                  <strong>{tx(`landing.about.promises.${key}.title`)}</strong>
                  <span className="muted text-sm">{tx(`landing.about.promises.${key}.text`)}</span>
                </span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

export function IndustriesSection() {
  return (
    <section id="industries" className="scroll-mt-20 py-20">
      <div className="mx-auto grid max-w-6xl items-center gap-14 px-4 lg:grid-cols-[1.1fr_0.9fr]">
        <div>
          <Heading title={t("landing.industries.heading")} text={t("landing.industries.lead")} />
          <ul className="m-0 flex list-none flex-wrap gap-3 p-0">
            {industries.map(({ key, icon: Icon }) => (
              <li
                key={key}
                className="group flex items-center gap-2 rounded-full border border-line bg-surface px-4 py-2.5 font-medium shadow-sm transition-colors hover:border-accent hover:bg-accent-solid hover:text-accent-fg"
              >
                <Icon aria-hidden="true" className="size-4 text-accent group-hover:text-accent-fg" />
                {tx(`landing.industries.${key}`)}
              </li>
            ))}
          </ul>
        </div>
        <Reveal>
          <div className="relative mx-auto w-full max-w-sm">
            <div
              aria-hidden="true"
              className="absolute -inset-3 rounded-[58%_42%_38%_62%/46%_55%_45%_54%] bg-accent-solid/15"
            />
            <div className="relative aspect-[5/6] overflow-hidden rounded-[58%_42%_38%_62%/46%_55%_45%_54%] shadow-2xl">
              <Photo name="teamCenter" />
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

const steps = ["brief", "shortlist", "interview", "join"] as const;

export function ProcessSection() {
  return (
    <section id="process" className="scroll-mt-20 border-y border-line bg-surface py-20">
      <div className="mx-auto max-w-6xl px-4">
        <Heading title={t("landing.process.heading")} text={t("landing.process.lead")} />
        <ol className="relative m-0 grid list-none gap-10 p-0 md:grid-cols-4 md:gap-6">
          <span aria-hidden="true" className="absolute left-5 right-5 top-5 hidden h-px bg-line md:block" />
          {steps.map((step, index) => (
            <li key={step} className="relative grid content-start gap-3">
              <span className="relative z-10 grid size-10 place-items-center rounded-full bg-accent-solid text-sm font-bold text-accent-fg shadow-md">
                {index + 1}
              </span>
              <h3 className="m-0 text-base">{tx(`landing.process.steps.${step}.title`)}</h3>
              <p className="muted m-0 text-sm">{tx(`landing.process.steps.${step}.text`)}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
