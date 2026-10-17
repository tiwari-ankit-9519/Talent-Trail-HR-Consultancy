import Link from "next/link";
import { Mail, MapPin, Phone } from "lucide-react";
import { industries, services } from "@/components/landing/content";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { site } from "@/lib/site";
import { t, tx } from "@/lib/messages";

export function ContactSection() {
  const details = [
    { icon: Mail, value: site.email, href: `mailto:${site.email}` },
    { icon: Phone, value: site.phone, href: `tel:${site.phone.replace(/\s/g, "")}` },
    { icon: MapPin, value: site.address, href: "" },
  ].filter((item) => item.value !== "");

  return (
    <section id="contact" className="scroll-mt-20 py-16">
      <div className="mx-auto max-w-6xl px-4">
        <Card className="grid gap-6 p-8 text-center sm:p-12">
          <h2 className="m-0 text-3xl font-bold tracking-tight">{t("landing.contact.heading")}</h2>
          <p className="muted mx-auto m-0 max-w-xl">{t("landing.contact.lead")}</p>
          {details.length > 0 && (
            <ul className="m-0 flex list-none flex-wrap justify-center gap-x-8 gap-y-3 p-0">
              {details.map(({ icon: Icon, value, href }) => (
                <li key={value} className="flex items-center gap-2">
                  <Icon aria-hidden="true" className="size-4 text-accent" />
                  {href ? <a href={href}>{value}</a> : <span>{value}</span>}
                </li>
              ))}
            </ul>
          )}
          <div className="flex flex-wrap justify-center gap-3">
            {site.email !== "" && (
              <Button asChild>
                <a href={`mailto:${site.email}`}>{t("landing.contact.hire")}</a>
              </Button>
            )}
            <Button asChild variant={site.email !== "" ? "outline" : "default"}>
              <Link href="/apply">{t("landing.contact.jobs")}</Link>
            </Button>
          </div>
        </Card>
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
