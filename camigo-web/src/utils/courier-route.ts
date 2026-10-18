import type { CourierRoute } from '@/types/courier-execution'

/** 人在当前快递站（或正前往的下一站）可取的订单，不要求该站是路线第一步。 */
export function stationPickupOrderIds(route: CourierRoute | null | undefined): number[] {
  if (!route?.steps.length) return []
  const atCurrent = route.steps
    .filter((step) => step.locationId === route.currentLocationId)
    .flatMap((step) => step.pickupOrderIds ?? [])
  if (atCurrent.length) return atCurrent
  return [...(route.steps[0]?.pickupOrderIds ?? [])]
}
