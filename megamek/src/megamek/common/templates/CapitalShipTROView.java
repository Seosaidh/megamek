/**
 *
 */
package megamek.common.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.Messages;
import megamek.common.Mounted;
import megamek.common.Warship;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestAdvancedAerospace;

/**
 * Creates a TRO template model for advanced aerospace units (jumpships,
 * warships, space stations)
 *
 * @author Neoancient
 *
 */
public class CapitalShipTROView extends AeroTROView {

    private final Jumpship aero;

    public CapitalShipTROView(Jumpship aero) {
        super(aero);
        this.aero = aero;
    }

    @Override
    protected String getTemplateFileName(boolean html) {
        if (html) {
            return "aero_vessel.ftlh";
        }
        return "aero_vessel.ftl";
    }

    @Override
    protected void initModel(EntityVerifier verifier) {
        addBasicData(aero);
        addArmor();
        setModelData("formatBayRow", new FormatTableRowMethod(new int[] { 8, 24, 10 },
                new Justification[] { Justification.LEFT, Justification.LEFT, Justification.LEFT }));
        setModelData("usesWeaponBays", aero.usesWeaponBays());
        final int nameWidth = addWeaponBays(ARCS);
        setModelData("formatWeaponBayRow",
                new FormatTableRowMethod(new int[] { nameWidth, 5, 8, 8, 8, 10, 12 },
                        new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER,
                                Justification.CENTER, Justification.CENTER, Justification.CENTER,
                                Justification.LEFT }));
        addFluff();
        final TestAdvancedAerospace testAero = new TestAdvancedAerospace(aero, verifier.aeroOption, null);

        setModelData("massDesc", aero.getWeight());
        setModelData("fuelMass", aero.getFuelTonnage());
        setModelData("fuelPoints", aero.getFuel());
        setModelData("safeThrust", aero.getWalkMP());
        setModelData("maxThrust", aero.getRunMP());
        setModelData("hsCount",
                aero.getHeatType() == Aero.HEAT_DOUBLE ? aero.getOHeatSinks() + " (" + (aero.getOHeatSinks() * 2) + ")"
                        : aero.getOHeatSinks());
        setModelData("si", aero.get0SI());
        setModelData("armorType", formatArmorType(aero, false).toLowerCase());
        setModelData("armorMass", testAero.getWeightArmor());
        setModelData("dropshipCapacity", aero.getDockingCollars().size());
        setModelData("escapePods", aero.getEscapePods());
        setModelData("lifeBoats", aero.getLifeBoats());
        setModelData("gravDecks", aero.getGravDecks().stream().map(size -> size + " m").collect(Collectors.toList()));
        setModelData("sailIntegrity", aero.hasSail() ? aero.getSailIntegrity() : Messages.getString("TROView.NA"));
        if (aero.getDriveCoreType() != Jumpship.DRIVE_CORE_NONE) {
            setModelData("kfIntegrity", aero.getKFIntegrity());
        }
        if (aero.getDriveCoreType() == Jumpship.DRIVE_CORE_PRIMITIVE) {
            setModelData("jumpRange", aero.getJumpRange());
        }
        final List<String> misc = new ArrayList<>();
        if (aero.hasLF()) {
            misc.add(Messages.getString("TROView.lfbattery"));
        }
        final Map<String, Integer> miscCount = aero.getMisc().stream()
                .filter(m -> (m.getLinked() == null) && (m.getLinkedBy() == null))
                .collect(Collectors.groupingBy(m -> m.getName(), Collectors.summingInt(m -> 1)));
        miscCount.forEach((k, v) -> misc.add(String.format("%d %s", v, k)));
        setModelData("miscEquipment", misc);
        setModelData("lfBattery", aero.hasLF());

        addTransportBays(aero);
        addAmmo();
        addCrew();
    }

    private void addFluff() {
        addEntityFluff(aero);
        final Map<String, String> dimensions = new HashMap<>();
        if (aero.getFluff().getLength().length() > 0) {
            dimensions.put("length", aero.getFluff().getLength());
        }
        if (aero.getFluff().getWidth().length() > 0) {
            dimensions.put("width", aero.getFluff().getWidth());
        }
        if (aero.getFluff().getHeight().length() > 0) {
            dimensions.put("height", aero.getFluff().getHeight());
        }
        if (!dimensions.isEmpty()) {
            setModelData("dimensions", dimensions);
        }
        if (aero.getFluff().getUse().length() > 0) {
            setModelData("use", aero.getFluff().getUse());
        }
    }

    private static final String[][] ARCS = { { "Nose" }, { "FRS", "FLS" }, { "RBS", "LBS" }, { "ARS", "ALS" },
            { "Aft" } };

    @Override
    protected String getArcAbbr(Mounted m) {
        switch (m.getLocation()) {
            case Aero.LOC_NOSE:
                return ARCS[0][0];
            case Jumpship.LOC_FRS:
                return ARCS[1][0];
            case Jumpship.LOC_FLS:
                return ARCS[1][1];
            case Jumpship.LOC_ARS:
                return ARCS[3][0];
            case Jumpship.LOC_ALS:
                return ARCS[3][1];
            case Aero.LOC_AFT:
                return ARCS[4][0];
            case Warship.LOC_RBS:
                return ARCS[2][0];
            case Warship.LOC_LBS:
                return ARCS[2][1];
        }
        return super.getArcAbbr(m);
    }

    private static final int[][] ARMOR_LOCS = { { Jumpship.LOC_NOSE }, { Jumpship.LOC_FRS, Jumpship.LOC_FLS },
            { Jumpship.LOC_ARS, Jumpship.LOC_ALS }, { Jumpship.LOC_AFT } };

    private void addArmor() {
        setModelData("armorValues", addArmorStructureEntries(aero, Entity::getOArmor, ARMOR_LOCS));
    }

    protected void addCrew() {
        setModelData("crew", new ArrayList<>());
        if (aero.getNOfficers() > 0) {
            addCrewEntry("officer", aero.getNOfficers());
        }
        final int nEnlisted = aero.getNCrew() - aero.getBayPersonnel() - aero.getNGunners() - aero.getNOfficers();
        if (nEnlisted > 0) {
            addCrewEntry("enlisted", nEnlisted);
        }
        if (aero.getNGunners() > 0) {
            addCrewEntry("gunner", aero.getNGunners());
        }
        if (aero.getBayPersonnel() > 0) {
            addCrewEntry("bayPersonnel", aero.getBayPersonnel());
        }
        if (aero.getNPassenger() > 0) {
            addCrewEntry("passenger", aero.getNPassenger());
        }
        if (aero.getNMarines() > 0) {
            addCrewEntry("marine", aero.getNMarines());
        }
        if (aero.getNBattleArmor() > 0) {
            addCrewEntry("baMarine", aero.getNBattleArmor());
        }
    }

}
