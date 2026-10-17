import ProfileDetailView from "@/components/ProfileDetailView";

export default async function PersonPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ProfileDetailView id={id} />;
}
