import {Behaviours} from 'entcore';
import {mailTemplateForm} from './sniplets';
import rights from './rights';

Behaviours.register('massmailing', {
    rights,
    sniplets: {
        'massmailing-manage/sniplet-template-form': mailTemplateForm
    }
});
