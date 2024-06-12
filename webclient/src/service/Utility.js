export default class Utility {
    static defaultPrefix = 'ca://';

    static pvTypeOptions = [
        { name: 'Channel Access', value: 'ca://' },
        { name: 'PV Access', value: 'pva://' },
    ];

    static prefixPV(pvname, prefix) {
        if(!pvname || !prefix) {
            return '';
        } else {
            return Utility.hasPrefix(pvname) ? pvname : prefix + pvname;
        }
    };

    static unprefixPV(pvname) {
        if(!pvname) {
            return '';
        } else {
            return Utility.hasPrefix(pvname) ? pvname.replace('ca://', '').replace('pva://', '') : pvname;
        }
    };

    static hasPrefix(pvname) {
        return pvname.includes('ca://') || pvname.includes('pva://');
    };
}