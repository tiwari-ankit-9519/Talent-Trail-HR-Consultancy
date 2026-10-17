import { ContactSection, LandingFooter } from "@/components/landing/ContactFooter";
import HeroSlider from "@/components/landing/HeroSlider";
import LandingHeader from "@/components/landing/LandingHeader";
import { AboutSection, IndustriesSection, ProcessSection, ServicesSection } from "@/components/landing/Sections";

export default function LandingView() {
  return (
    <div className="landing-shell min-h-screen bg-bg">
      <LandingHeader />
      <main>
        <HeroSlider />
        <ServicesSection />
        <AboutSection />
        <IndustriesSection />
        <ProcessSection />
        <ContactSection />
      </main>
      <LandingFooter />
    </div>
  );
}
