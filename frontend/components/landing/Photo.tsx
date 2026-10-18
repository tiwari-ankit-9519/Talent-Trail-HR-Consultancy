import { tx } from "@/lib/messages";

export const photos = {
  teamWide: { src: "/landing/team-wide.webp", width: 1600, height: 868 },
  teamCenter: { src: "/landing/team-center.webp", width: 800, height: 960 },
  teamLeft: { src: "/landing/team-left.webp", width: 800, height: 761 },
  teamRight: { src: "/landing/team-right.webp", width: 800, height: 1006 },
  man: { src: "/landing/man.webp", width: 800, height: 1184 },
  bannerTalent: { src: "/landing/banner-talent.webp", width: 1800, height: 647 },
  bannerTeams: { src: "/landing/banner-teams.webp", width: 1800, height: 640 },
  bannerResources: { src: "/landing/banner-resources.webp", width: 1800, height: 640 },
} as const;

export type PhotoName = keyof typeof photos;

export default function Photo({
  name,
  className = "",
  decorative = false,
  priority = false,
}: {
  name: PhotoName;
  className?: string;
  decorative?: boolean;
  priority?: boolean;
}) {
  const photo = photos[name];
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={photo.src}
      width={photo.width}
      height={photo.height}
      alt={decorative ? "" : tx(`landing.images.${name}`)}
      loading={priority ? "eager" : "lazy"}
      decoding="async"
      draggable={false}
      className={`block size-full object-cover ${className}`}
    />
  );
}
