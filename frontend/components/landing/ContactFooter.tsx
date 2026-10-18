import Link from "next/link";
import { Mail, MapPin, Phone } from "lucide-react";
import { industries, services } from "@/components/landing/content";
import Photo from "@/components/landing/Photo";
import { Button } from "@/components/shadcn/button";
import { site } from "@/lib/site";
import { t, tx } from "@/lib/messages";

export function ContactSection() {
  const details = [
    { icon: Mail, value: site.email, href: `mailto:${site.email}` },
    { icon: Phone, value: site.phone, href: `tel:${site.phone.replace(/\s/g, "")}` },
    { icon: MapPin, value: site.address, href: "" },
  ].filter((item) => item.value !== "");

  return (
    <section id="contact" className="scroll-mt-20 px-4 py-20">
      <div className="relative isolate mx-auto max-w-6xl overflow-hidden rounded-[2rem] shadow-2xl">
        <div aria-hidden="true" className="absolute inset-0 -z-20">
          <Photo name="teamWide" decorative />
        </div>
        <div
          aria-hidden="true"
          className="absolute inset-0 -z-10 bg-gradient-to-r from-indigo-950/95 via-indigo-900/85 to-indigo-700/30"
        />
        <div className="grid max-w-xl gap-6 px-6 py-16 text-white sm:px-14 sm:py-24">
          <h2 className="m-0 text-3xl font-bold tracking-tight text-white sm:text-4xl">
            {t("landing.contact.heading")}
          </h2>
          <p className="m-0 text-lg text-white/80">{t("landing.contact.lead")}</p>
          {details.length > 0 && (
            <ul className="m-0 flex list-none flex-wrap gap-x-8 gap-y-3 p-0">
              {details.map(({ icon: Icon, value, href }) => (
                <li key={value} className="flex items-center gap-2">
                  <Icon aria-hidden="true" className="size-4 text-white/70" />
                  {href ? (
                    <a href={href} className="text-white">
                      {value}
                    </a>
                  ) : (
                    <span>{value}</span>
                  )}
                </li>
              ))}
            </ul>
          )}
          <div className="flex flex-wrap gap-3">
            {site.email !== "" && (
              <Button asChild className="bg-white text-slate-900 hover:bg-white/90">
                <a href={`mailto:${site.email}`}>{t("landing.contact.hire")}</a>
              </Button>
            )}
            <Button
              asChild
              variant="outline"
              className={
                site.email !== ""
                  ? "border-white/50 bg-transparent text-white hover:bg-white/15 hover:text-white"
                  : "border-transparent bg-white text-slate-900 hover:bg-white/90 hover:text-slate-900"
              }
            >
              <Link href="/apply">{t("landing.contact.jobs")}</Link>
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
}

export function LandingFooter() {
  return (
    <footer className="border-t border-line bg-surface">
      <div className="mx-auto grid max-w-6xl gap-8 px-4 py-12 sm:grid-cols-2 lg:grid-cols-4">
        <div className="grid content-start gap-3">
          <strong>{t("app.title")}</strong>
          <p className="muted m-0 text-sm">{t("landing.footer.about")}</p>
        </div>
        <div className="grid content-start gap-2 text-sm">
          <strong>{t("landing.footer.quick")}</strong>
          <Link href="/#about">{t("landing.nav.about")}</Link>
          <Link href="/#services">{t("landing.nav.services")}</Link>
          <Link href="/#industries">{t("landing.nav.industries")}</Link>
          <Link href="/#process">{t("landing.nav.process")}</Link>
          <Link href="/apply">{t("landing.nav.jobs")}</Link>
          <Link href="/login">{t("landing.nav.signIn")}</Link>
        </div>
        <div className="grid content-start gap-2 text-sm">
          <strong>{t("landing.footer.services")}</strong>
          {services.map((service) => (
            <Link key={service.key} href="/#services">
              {tx(`landing.services.${service.key}.title`)}
            </Link>
          ))}
        </div>
        <div className="grid content-start gap-2 text-sm">
          <strong>{t("landing.footer.industries")}</strong>
          {industries.slice(0, 5).map((industry) => (
            <Link key={industry.key} href="/#industries">
              {tx(`landing.industries.${industry.key}`)}
            </Link>
          ))}
        </div>
      </div>
      <div className="border-t border-line py-4 text-center text-sm text-muted">
        {t("landing.footer.copyright").replace("{year}", String(new Date().getFullYear()))}
      </div>
    </footer>
  );
}
