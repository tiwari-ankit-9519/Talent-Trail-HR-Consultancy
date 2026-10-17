import PlacementDetailView from "@/components/PlacementDetailView";

export default async function PlacementDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <PlacementDetailView id={id} />;
}
