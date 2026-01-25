import React from 'react';
import { Shield, Code2, Cpu } from 'lucide-react';
import { Language } from '../types';
import { TRANSLATIONS } from '../utils/translations';

interface TeamProps {
  language: Language;
}

export const Team: React.FC<TeamProps> = ({ language }) => {
  const t = TRANSLATIONS[language];

  const teamMembers = [
    {
      nickname: "Michal92299",
      realName: "Michał Kaczmarzyk",
      role: t.role_founder,
      image: "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/michal92299.png",
      icon: Shield,
      color: "text-red-500",
      borderColor: "border-red-500/50"
    },
    {
      nickname: "RafeNop",
      realName: "Rafał Kaczmarzyk",
      role: t.role_cofounder,
      image: "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/RafeNop.png",
      icon: Code2,
      color: "text-blue-500",
      borderColor: "border-blue-500/50"
    }
  ];

  return (
    <div className="pb-24 pt-2">
      <div className="px-6 mb-8">
        <h2 className="text-3xl font-mono font-bold text-white mb-1">{t.header_team}</h2>
        <p className="text-muted text-sm">{t.sub_team}</p>
      </div>

      <div className="px-4 space-y-6">
        {teamMembers.map((member, index) => {
          const Icon = member.icon;
          return (
            <div 
              key={index}
              className={`
                relative overflow-hidden rounded-2xl bg-card/40 backdrop-blur-md border border-white/5 
                p-6 flex flex-col items-center text-center group transition-all duration-300 hover:bg-card/60
              `}
            >
              {/* Decorative background glow */}
              <div className={`absolute top-0 inset-x-0 h-32 opacity-20 bg-gradient-to-b from-${member.color.split('-')[1]}-500 to-transparent blur-2xl pointer-events-none`} />

              <div className={`relative w-32 h-32 mb-4 rounded-full p-1 border-2 ${member.borderColor} shadow-[0_0_20px_-5px_rgba(0,0,0,0.5)]`}>
                <img 
                  src={member.image} 
                  alt={member.nickname} 
                  className="w-full h-full rounded-full object-cover bg-black/50"
                />
                <div className="absolute bottom-0 right-0 p-2 bg-card rounded-full border border-white/10 shadow-lg">
                  <Icon size={16} className={member.color} />
                </div>
              </div>

              <h3 className="text-2xl font-bold text-white tracking-wide mb-1">
                {member.nickname}
              </h3>
              <p className="text-sm font-mono text-muted mb-3">{member.realName}</p>
              
              <div className={`px-4 py-1.5 rounded-full bg-white/5 border border-white/10 text-xs font-bold uppercase tracking-wider ${member.color}`}>
                {member.role}
              </div>
            </div>
          );
        })}

        <div className="mt-8 p-6 rounded-2xl border border-white/5 bg-white/5 text-center">
          <Cpu className="mx-auto text-primary mb-3" size={32} />
          <p className="text-xs text-muted font-mono leading-relaxed">
            HackerOS is a community-driven project dedicated to ethical hacking and cybersecurity enthusiasts.
          </p>
        </div>
      </div>
    </div>
  );
};
