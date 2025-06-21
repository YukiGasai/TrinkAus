import { toast } from "react-toastify";
import { addHydration } from "../api/addHydration";
import { dateToString, getUnitStringWithValue } from "./unitHelper";
import { currentDate, currentHydration } from "./signals";

export const handleAddHydration = async (amount: number) => {

    console.log("Adding hydration:", amount, "at", dateToString(currentDate.peek()));
    const addResult = await addHydration(amount, dateToString(currentDate.peek()));
    if (addResult._tag === "Failure") {
        toast.error(addResult.error);
        return;
    }
    toast.success(`Added ${getUnitStringWithValue(amount)} of water successfully!`);

    currentHydration.value = addResult.data;
}
