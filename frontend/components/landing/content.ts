import {
  Briefcase,
  Building2,
  Clock,
  Cpu,
  Factory,
  Headset,
  HeartPulse,
  ShoppingCart,
  Store,
  Telescope,
  Users,
  type LucideIcon,
} from "lucide-react";

export type ServiceKey = "permanent" | "contract" | "rpo" | "executive";

export const services: { key: ServiceKey; icon: LucideIcon }[] = [
  { key: "permanent", icon: Briefcase },
  { key: "contract", icon: Clock },
  { key: "rpo", icon: Users },
  { key: "executive", icon: Telescope },
];

export type IndustryKey = "manufacturing" | "retail" | "ecommerce" | "healthcare" | "it" | "bpo" | "leadership";

export const industries: { key: IndustryKey; icon: LucideIcon }[] = [
  { key: "manufacturing", icon: Factory },
  { key: "retail", icon: Store },
  { key: "ecommerce", icon: ShoppingCart },
  { key: "healthcare", icon: HeartPulse },
  { key: "it", icon: Cpu },
  { key: "bpo", icon: Headset },
  { key: "leadership", icon: Building2 },
];

export const slideIcons: LucideIcon[] = [Briefcase, Clock, Users];
