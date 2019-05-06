import {Behaviours} from 'entcore';
import {navigation} from './sniplets'
import rights from './rights';

Behaviours.register('presences', {
    rights,
    sniplets: {
        navigation
    }
});
