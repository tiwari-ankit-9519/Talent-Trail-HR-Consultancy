import { BadgeCheck, RefreshCw, ShieldCheck } from "lucide-react";
import { MatchArt } from "@/components/landing/art";
import Reveal from "@/components/landing/Reveal";
import { industries, services } from "@/components/landing/content";
import { Badge } from "@/components/shadcn/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/shadcn/card";
import { t, tx } from "@/lib/messages";

function Heading({ eyebrow, title, text }: { eyebrow: string; title: string; text?: string }) {
  return (
    <div className="mx-auto mb-10 grid max-w-2xl gap-3 text-center">
      <Badge variant="accent" className="mx-auto w-fit">
        {eyebrow}
      </Badge>
      <h2 className="m-0 text-3xl font-bold tracking-tight">{title}</h2>
      {text && <p className="muted m-0">{text}</p>}
    </div>
  );
}

export function ServicesSection() {
  return (
    <section id="services" className="scroll-mt-20 py-16">
      <div className="mx-auto max-w-6xl px-4">
        <Heading
          eyebrow={t("landing.services.eyebrow")}
          title={t("landing.services.heading")}
          text={t("landing.services.lead")}
        />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {services.map(({ key, icon: Icon }, position) => (
            <Reveal key={key} delay={position * 90} className="h-full">
              <Card className="h-full transition-transform duration-200 hover:-translate-y-1">
                <CardHeader>
                  <div className="mb-2 grid size-11 place-items-center rounded-xl bg-accent-solid text-accent-fg">
                    <Icon aria-hidden="true" className="size-5" />
                  </div>
                  <CardTitle>{tx(`landing.services.${key}.title`)}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="muted m-0 text-sm">{tx(`landing.services.${key}.text`)}</p>
                </CardContent>
              </Card>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

const promises = [
  { key: "pay", icon: BadgeCheck },
  { key: "replace", icon: RefreshCw },
  { key: "consent", icon: ShieldCheck },
] as const;

export function AboutSection() {
  return (
    <section id="about" className="scroll-mt-20 border-y border-line bg-surface py-16">
      <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 lg:grid-cols-2">
        <div className="grid gap-4">
          <Badge variant="accent" className="w-fit">
            {t("landing.about.eyebrow")}
          </Badge>
          <h2 className="m-0 text-3xl font-bold tracking-tight">{t("landing.about.heading")}</h2>
          <p className="muted m-0">{t("landing.about.p1")}</p>
          <p className="muted m-0">{t("landing.about.p2")}</p>
          <div className="mt-2 max-w-md">
            <MatchArt />
          </div>
        </div>
        <div className="grid gap-3">
          {promises.map(({ key, icon: Icon }, position) => (
            <Reveal key={key} delay={position * 120}>
              <Card className="flex items-start gap-4 p-5 transition-transform duration-200 hover:translate-x-1">
                <div className="grid size-10 shrink-0 place-items-center rounded-xl bg-accent-solid text-accent-fg">
                  <Icon aria-hidden="true" className="size-5" />
                </div>
                <div className="grid gap-1">
                  <strong>{tx(`landing.about.promises.${key}.title`)}</strong>
                  <span className="muted text-sm">{tx(`landing.about.promises.${key}.text`)}</span>
                </div>
              </Card>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

export function IndustriesSection() {
  return (
    <section id="industries" className="scroll-mt-20 py-16">
      <div className="mx-auto max-w-6xl px-4">
        <Heading
          eyebrow={t("landing.industries.eyebrow")}
          title={t("landing.industries.heading")}
          text={t("landing.industries.lead")}
        />
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {industries.map(({ key, icon: Icon }, position) => (
            <Reveal key={key} delay={position * 60}>
              <Card className="group flex items-center gap-3 p-4 transition-all duration-200 hover:-translate-y-1 hover:border-accent">
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-accent-solid/10 text-accent transition-colors group-hover:bg-accent-solid group-hover:text-accent-fg">
                  <Icon aria-hidden="true" className="size-5" />
                </span>
                <span className="font-medium">{tx(`landing.industries.${key}`)}</span>
              </Card>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

const steps = ["brief", "shortlist", "interview", "join"] as const;

export function ProcessSection() {
  return (
    <section id="process" className="scroll-mt-20 border-y border-line bg-surface py-16">
      <div className="mx-auto max-w-6xl px-4">
        <Heading
          eyebrow={t("landing.process.eyebrow")}
          title={t("landing.process.heading")}
          text={t("landing.process.lead")}
        />
        <ol className="m-0 grid list-none gap-4 p-0 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map((step, index) => (
            <li key={step}>
              <Reveal delay={index * 120} className="h-full">
                <Card className="h-full p-5 transition-transform duration-200 hover:-translate-y-1">
                  <span className="mb-3 grid size-9 place-items-center rounded-full bg-accent-solid text-sm font-bold text-accent-fg">
                    {index + 1}
                  </span>
                  <h3 className="m-0 mb-1 text-base">{tx(`landing.process.steps.${step}.title`)}</h3>
                  <p className="muted m-0 text-sm">{tx(`landing.process.steps.${step}.text`)}</p>
                </Card>
              </Reveal>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
