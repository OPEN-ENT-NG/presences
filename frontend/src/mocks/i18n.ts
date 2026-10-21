import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

/**
 * Tests only: i18next without backend nor bundle, so the components display
 * the i18n keys right away (no request to load the translations).
 */
i18n.use(initReactI18next).init({
  defaultNS: 'common',
  ns: ['common', 'presences'],
  resources: {},
  fallbackLng: 'fr',
  lng: 'fr',
  interpolation: {
    escapeValue: false,
    prefix: '[[',
    suffix: ']]',
  },
});

export default i18n;
